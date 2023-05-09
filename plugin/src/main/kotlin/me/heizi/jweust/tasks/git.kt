package me.heizi.jweust.tasks

import kotlinx.coroutines.runBlocking
import me.heizi.jweust.JweustTasks
import me.heizi.jweust.tasks.Git.throws
import me.heizi.kotlinx.shell.*
import java.io.File
import java.io.IOException

private inline val CommandResult.isSucceed: Boolean
    get () = this is CommandResult.Success

internal fun JweustTasks.git() {
    var state = "started"
    while (state != "done") {
        _logger.lifecycle("> Task :jweust:git: $state")
        state = nextStateOf(state)
            ?: throw IllegalStateException("state $state is not supported")
    }
    _logger.lifecycle("> Task :jweust:git: OjbK")

}

/**
 * check state of repository and return it
 *
 * actions:
 *
 * - clone repo or fetch
 * - check the latest tag and compare it with local tag.  @main branch
 * - checkout or create a branch for subproject by name.
 * - parse files and commit if its creating or parsed file has changed.
 * - check tag of branch if it's deprecated
 * - merge branch to tag
 *
 * - merge before :: human visiting required
 * - checkout
 * ...
 */
internal fun JweustTasks.nextStateOf(state:String):String? = when(state) {
    "started" -> {
        if (!jweustRoot.exists()) "clone"
        else {
            Git.root = jweustRoot
            require(jweustRoot.isDirectory) {
                "jweust root must be exists"
            }
            require(jweustRoot.listFiles()?.isNotEmpty() == true) {
                "jweust root must not be empty"
            }
            Git checkout "main"
            Git branch this
            if (Git.latestResult.isSucceed) {
//                "branch-created"
            }
            Git checkout rustProjectName
            val head = jweustRoot.resolve(".git")
                .resolve("HEAD").takeIf { it.exists() }!!
                .readText()
                .lines()
                .first { it.isNotEmpty() }
            require(head.startsWith("ref: refs/heads/")) {
                "jweust is not on a branch\n|$head|"
            }
            "checking-merging-state"
        }
    }
    "checking-merging-state" -> {
        val isMerging = jweustRoot.resolve(".git")
            .resolve("MERGE_HEAD")
            .exists()
        require(!isMerging) {
            "jweust is merging, please solve it manually"
        }
        "no-merge-problem"
    }
    "no-merge-problem" -> "fetching"
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
        _logger.warn("jweust version ${Git.repoTag} is deprecated, please update your jweust to current version ${Git.latestTag} " +
                "you can clean the path after you update: ${jweustRoot.absolutePath} .\n" +
                ""
        )
        "checking-header-is-merged-tag"
    }
    "checking-header-is-merged-tag"-> {
        Git branch "--contains ${Git.repoTag}"
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
        Git merge "tags/${Git.repoTag}"
        throw NotImplementedError(
            "merging to tag now, please solve conflicts manually if it's needed. " +
                    "after that, please run this task again. the path is ${jweustRoot.absolutePath} . "
        )
    }
    "merged" -> "parse"
    "tag-is-current" -> "parse"
    "parse" -> if(parse()) "commit" else "done"
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


private object Git {

    const val repoTag = "0.0.1"

    val isTagDeprecated: Boolean by lazy {
        latestTag != repoTag
    }
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
        "git clone -b $repoTag $repo ${workdir.absolutePath}"().throws()
    }

    infix fun add(path: String) = wrapper {
        "git add $path"()
    }
    fun tag() = runBlocking {
        "git tag"().throws().message.lines()
    }
    // lines[1] nothing to commit, working tree clean
//    fun status() = runBlocking {
//        "git status"().throws().message.lines()
//    }
    // Automatic merge failed; fix conflicts and then commit the result.
    infix fun merge(branch:String) = wrapper {
        "git merge $branch"()
    }



//    infix fun checkout(jweustTasks: JweustTasks)
//        = checkout(jweustTasks.rustProjectName)
    // error: pathspec 'b' did not match any file(s) known to git
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

}