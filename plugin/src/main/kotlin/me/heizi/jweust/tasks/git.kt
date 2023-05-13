package me.heizi.jweust.tasks

import kotlinx.coroutines.runBlocking
import me.heizi.jweust.JweustTasks
import org.gradle.api.Project
import java.io.File
import java.io.IOException
import java.io.OutputStream


/**
 * It will generate a git repo, checkout, commit, merge automatically.
 * By String stage mode design, it's readable.
 *
 * # flow
 * ```
 * started -> clone -> started
 *         -> new-branch-created -> checkout-branch
 *         -> checkout-branch
 * checkout-branch -> checking-merging-state
 *
 * checking-merging-state -> no-merge-problem
 *                        -> throw exception -> x
 *                           (restart task) -> merged -> parse
 *
 * no-merge-problem -> skip-update-tag -> parse
 *                  # updating
 *                  -> fetching -> checking-tags -> tag-is-current -> parse
 *                                 checking-tags -> tag-deprecated
 * tag-deprecated -> checking-header-is-merged-tag -> merge-to-tag -> throw and stop -> x
 *                -> ( wait for next time starting task and **no-merge-problem** )
 * parse -> commit -> done
 *       -> done
 * ```
 */
internal fun TaskUpdateRepo.generateValidatedRustProject() {
    Git.project = this.project
    Git.root = jweustRoot
    Git.isRepoOrThrows()
    var state = "started"
    while (state != "done") {
        _logger.lifecycle("> jweust configuration --$state")
        state = nextStateOf(state)
            ?: throw IllegalStateException("state $state is not supported")
    }
    _logger.lifecycle("> Task :jweust:git: OjbK")
}
/**
 * check the stage of jweust git repo and manage it, for saving the result of rust project or update.
 *
 * @see generateValidatedRustProject
 */
private fun TaskUpdateRepo.nextStateOf(state:String):String? = when(state) {
    "started" -> if (!hashRepo)  "clone" else {
        Git.throwsNext()
        Git.dismissNext()
        Git checkout "main"
        Git.dismissNext()
        Git branch this
        if (Git.latestResult.isSuccess)
            "new-branch-created"
        else "checkout-branch"
    }
    "new-branch-created" -> {
        ignoreRemoveVarRs()
        Git add "."
        Git commit "init $rustProjectName"
        "checkout-branch"
    }
    "checkout-branch" -> {
        Git checkout rustProjectName
        Git throwIfNotBranchOf rustProjectName
        "checking-merging-state"
    }
    "checking-merging-state" -> {
        require(!Git.isMerging) {
            "jweust is merging, please solve it manually"
        }
        "no-merge-problem"
    }
    "no-merge-problem" -> {
        if (isUpdateTag) "skip-update-tag" else "fetching"
    }
    "fetching" -> {
        Git.fetch()
        "checking-tags"
    }
    "checking-tags" -> {
        if(Git.isTagDeprecated)
            "tag-deprecated"
        else "tag-is-current"
    }
    "tag-deprecated"   -> {
        if (isDeprecatedWarnDspIng) _logger.warn(
            "jweust version ${Git.currentTag} is deprecated, please update your jweust to current version ${Git.latestTag} " +
                "you can clean the path after you update: `${jweustRoot.absolutePath}` .\n" +
                "also you can disable new version checking by setting ext `jweust.git.update-tag`as `false` in your project.\n" +
                "to disable this warning, please set ext `jweust.git.deprecated.warn` as `false` in your project."
        )
        "checking-header-is-merged-tag"
    }
    "checking-header-is-merged-tag"-> {
        Git.throwsNext()
        Git branch "--contains ${Git.currentTag}"
        val contained: Boolean = Git.latestResult
            .map {
                it.stdout.lines().map { it.trim('*',' ') }
                    .contains(rustProjectName)
            }.onFailure {
                it.printStackTrace()
            }.getOrDefault(true)
        if (!contained)
            "merge-to-tag"
        else "merged"
    }
    "merge-to-tag"  -> {
        Git merge "tags/${Git.currentTag}"
        throw NotImplementedError(
            "merging to tag now, please solve conflicts manually if it's needed. " +
                    "after that, please run this task again. the path is ${jweustRoot.absolutePath} . "
        )
    }
    "merged" -> "parse"
    "skip-update-tag" -> "parse"
    "tag-is-current" -> "parse"
    "parse" -> if(updateFiles()) "commit" else "done"
    "commit" -> {
        Git.throwsNext()
        Git add "."
        Git.throwsNext()
        Git commit "update ${System.nanoTime()}"
        "done"
    }
    "clone" -> {
        require(!jweustRoot.exists()) {
            "jweust root must not exists"
        }
        Git cloneInto jweustRoot
        "started"
    }
    else -> null
}
typealias GitResult = Triple<String,String,Int>
typealias GitResultW = Result<GitResult>
private inline val GitResult.isSucceed get() = code == 0
private inline val GitResult.stdout get() = first
private inline val GitResult.stderr get() = second
private inline val GitResult.code get() = third
class GitException(
    command:String,
    msg: String,
//    code: Int?=null,
    cause: Throwable? = null
) : RuntimeException("""
    |`$command` not succeed cuz :
    |$msg
    |""".trimMargin(),
cause)

@Suppress("UNUSED_PARAMETER")
private object Git {

    const val currentTag = "0.0.3"

    lateinit var project: Project

    @Suppress("NAME_SHADOWING")
    val latestTag by lazy {
        tag().sortedWith { o1, o2 ->
            val o1 = o1.split(".").map { it.toIntOrNull() ?: 0 }
            val o2 = o2.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in o1.indices) {
                if (o1[i] != o2[i])
                    return@sortedWith o1[i].compareTo(o2[i])
            }
            255
        }.last()
    }

    val isTagDeprecated get() = latestTag != currentTag


    const val repo = "git@github.com:ElisaMin/Jweust-template.git"

    var latestResult: GitResultW = Result.failure(IllegalStateException("git not run yet"))


    var isDismissNextTime = false
    var isDismissErrNextTime = false
    var isThrowNetTime = false

    var root = File(".")
    private inline fun  wrapper(crossinline block:()->GitResultW) {
        runCatching {
            latestResult = block()
        }.onFailure {
            throw IllegalStateException(it)
        }
    }
    fun GitResultW.throws(): GitResult {
        latestResult = this
        onFailure {
            throw it
        }
        return getOrThrow()
    }


    infix fun cloneInto(workdir: File) = wrapper {
        throwsNext()
        "git clone -b $currentTag $repo ${workdir.absolutePath}"()
    }

    infix fun add(path: String) = wrapper {
        "git add $path"()
    }
    fun tag() = runBlocking {
        "git tag"().map {
            it.stdout.lines()
        }.getOrThrow()
    }

    infix fun merge(branch:String) = wrapper {
        "git merge $branch"()
    }

    infix fun checkout(repoTag: String) = wrapper {
        "git checkout $repoTag"()
    }

    infix fun commit(msg: String)  = wrapper {
        "git commit -m \"$msg\""()
    }
    private operator fun String.invoke() = project.runCatching {
        val msg = StringBuffer()
        val out = StringBuffer()
        val err = StringBuffer()
        val char = '\n'.code
        runCatching { exec {
            errorOutput = object : OutputStream() {
                override fun write(b: Int) {
                    if (b == char) {
                        val i = err.lastIndexOf('\n')
                            .takeIf { it != -1 } ?: 0
                        val s = err.substring(i)
                        msg.append(s)
                    }
                    err.append(b.toChar())
                    if (isDismissNextTime || isDismissErrNextTime) return
                    System.err.write(b)
                }
            }
            standardOutput = object : OutputStream() {
                override fun write(b: Int) {
                    if (b == char) {
                        val i = out.lastIndexOf('\n')
                            .takeIf { it != -1 } ?: 0
                        val s = out.substring(i)
                        msg.append(s)
                    }
                    out.append(b.toChar())
                    if (isDismissNextTime) return
                    System.out.write(b)
                }
            }
            workingDir = root
            commandLine("cmd", "/c", this@invoke)
        }.rethrowFailure() }.onFailure {
            throw GitException(this@invoke, msg.toString(), it)
        }.map {
            GitResult(out.toString(), err.toString(), it.exitValue)
        }.getOrThrow()
    }.onFailure {
        if(isThrowNetTime) throw it
    }.also {
        isThrowNetTime = false
        isDismissNextTime = false
        isDismissErrNextTime = false
    }

    fun fetch() = wrapper {
        "git fetch --all --tags"()
    }

    infix fun branch(branchName: String) = wrapper {
        "git branch $branchName"()
    }

    infix fun branch(jweustTasks: JweustTasks)
        = branch(jweustTasks.rustProjectName)

    infix fun throwIfNotBranchOf(branch: String) {
        val head = root.resolve(".git")
            .resolve("HEAD").takeIf { it.exists() }!!
            .readText()
            .lines()
            .first { it.isNotEmpty() }
        require(head.startsWith("ref: refs/heads/$branch")) {
            "jweust is not on a branch\n|$head|"
        }
    }

    val isMerging get() = root.resolve(".git")
        .resolve("MERGE_HEAD")
        .exists()
    fun isRepoOrThrows() {
        require(root.isDirectory) {
            "jweust root must be exists"
        }
        val files = root.listFiles().takeIf { it?.isNotEmpty() == true }
        require(files!=null) {
            "jweust root must not be empty : ${root.absolutePath}"
        }
        val git = files.find { it.name == ".git" }
        require(
            git!=null && git.isDirectory
                    && git.listFiles()?.isNotEmpty() == true
        )  {
            "jweust root must be a git repository : ${root.absolutePath}"
        }
    }
    fun hashRepoOrThrow() = root.run has@{
        if (!exists()&&!isDirectory) return@has false
        val fileNames = listFiles()?.takeIf { it.isNotEmpty() }?.map { it.name }
        if (fileNames==null) {
            require(delete())
            return@has false
        }
        val req = mutableListOf(
            "LICENSE",
            ".git",
            "Cargo.toml",
            "src",
        )

        val throws = {

            throw IOException(
                "jweust root is not a valid repository, " +
                        "please delete it and run this task again. " +
                        "the path is $absolutePath . " +
                        "missing files : $req"
            )
        }
        for (f in fileNames) {
            if (f in req) {
                req.remove(f)
            }
        }
        if (req.isNotEmpty())
            throws()
        req.addAll(arrayOf(
            "charsets.rs",
            "includes.rs",
            "jvm.rs",
            "logs.rs",
            "main.rs",
            "mod.rs",
        ))

        resolve("src").listFiles()
            ?.map { it.name }
            ?.forEach {
                if (it in req) { req.remove(it) }
        }
        return@has if (req.isNotEmpty())
            throws()
        else true
    }

    fun throwsNext() {
        isThrowNetTime = true
    }

    fun dismissNext() {
        isDismissNextTime = true
    }


}

private val JweustTasks.hashRepo: Boolean get() =
    runCatching {
        Git.root = jweustRoot
        Git.hashRepoOrThrow()
    }.getOrElse {
        if (getExtra("jweust.root-files.delete-always") != true ) {
            require(jweustRoot.delete())
            return true
        }
        throw IllegalStateException("repo has files, but not validate. you can set the extra properties `jweust.root-files.delete-always` as true to enable deleting",it)
    }

private val JweustTasks.isDeprecatedWarnDspIng: Boolean
    get() = getExtra("jweust.git.deprecated.warn") != false
private val JweustTasks.isUpdateTag: Boolean
    get() = getExtra("jweust.git.update-tag") != false
