package me.heizi.jweust.tasks

import kotlinx.coroutines.runBlocking
import me.heizi.jweust.JweustTasks
import me.heizi.jweust.tasks.Git.throws
import me.heizi.kotlinx.shell.*
import java.io.File
import java.io.IOException


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
internal fun JweustTasks.generateValidatedRustProject() {
    var state = "started"
    while (state != "done") {
        _logger.lifecycle("> Task :jweust:git: $state")
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
private fun JweustTasks.nextStateOf(state:String):String? = when(state) {
    "started" -> if (!hashRepo)  "clone" else {
        Git.root = jweustRoot
        Git.isRepoOrThrows()
        Git checkout "main"
        Git branch this
        if (Git.latestResult.isSucceed)
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
        Git branch "--contains ${Git.currentTag}"
        val contained = Git.latestResult.runCatching {
            throws().message.lines().
            map { it.trim('*',' ') }.contains(rustProjectName)
        }.onFailure {
            it.printStackTrace()
        }.getOrNull() == true
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
        Git add "."
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

@Suppress("UNUSED_PARAMETER")
private object Git {

    const val currentTag = "0.0.3"

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

    var latestResult = runBlocking {
        Shell("echo Hello World").await()
    }
    var root = File(".")
    private inline fun <T> wrapper(crossinline block:suspend ()->T) {
        runCatching { runBlocking {
            block().let {
                if (it is CommandResult) {
                    latestResult = it
                }
            }
        } }.onFailure {
            throw IllegalStateException("git failed",it)
        }
    }
    fun CommandResult.throws(): CommandResult.Success {
        latestResult = this
        if (this is CommandResult.Failed){
            val err = "$processingMessage\n$errorMessage"
            throw IOException(err)
        }
        return this as CommandResult.Success
    }


    infix fun cloneInto(workdir: File) = wrapper {
        "git clone -b $currentTag $repo ${workdir.absolutePath}"().throws()
    }

    infix fun add(path: String) = wrapper {
        "git add $path"()
    }
    fun tag() = runBlocking {
        "git tag"().throws().message.lines()
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

    @OptIn(ExperimentalApiReShell::class)
    private suspend inline operator fun String.invoke(): CommandResult = ReShell(
        this, workdir = root
    ).await()

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

}

private val JweustTasks.hashRepo: Boolean get() = jweustRoot.run has@{

    if (!exists()&&!isDirectory) return@has false
    val fileNames = listFiles()?.takeIf { it.isNotEmpty() }?.map { it.name }

    val deleteAndThrow = {
        if (!delete()) {
            throw IOException("jweust root is not a directory and can't be deleted")
        }
    }

    if (fileNames==null) {
        deleteAndThrow()
        return@has false
    }

    val hasRepo = fileNames.size>5
            && "LICENSE" in fileNames
            && ".git" in fileNames

    if (hasRepo) return@has true

    if (getExtra("jweust.root-files.delete-always") != true ) {
        deleteAndThrow()
        return@has false
    }
    error("jweust root must be empty or contains only .git folder,but it contains ${fileNames.joinToString()}. " +
            "but somehow, you can set jweust.root-files.delete-always to true to delete it automatically ."
    )

}
private val JweustTasks.isDeprecatedWarnDspIng: Boolean
    get() = getExtra("jweust.git.deprecated.warn") != false
private val JweustTasks.isUpdateTag: Boolean
    get() = getExtra("jweust.git.update-tag") != false
private inline val CommandResult.isSucceed: Boolean
    get () = this is CommandResult.Success
