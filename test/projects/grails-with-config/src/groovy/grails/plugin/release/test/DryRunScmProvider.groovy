package grails.plugin.release.test

class DryRunScmProvider {
    File baseDir
    def interactive

    DryRunScmProvider(baseDir, interactive) {
        this.baseDir = baseDir as File
    }

    void importIntoRepo(String hostUrl, String msg = "") {
        interactive.out.println "Import project code into repository '$hostUrl' with commit message '$msg'"
    }
   
    boolean isManaged(File fileOrDir = null) {
        if (!fileOrDir) fileOrDir = baseDir

        interactive.out.println "Checking whether '${fileOrDir}' is managed or not."
        return true
    }

    List getUnmanagedFiles() {
        return []
    }

    void manage(File fileOrDir = null) {
        if (!fileOrDir) fileOrDir = baseDir
        interactive.out.println "Now managing $fileOrDir"
    }

    boolean upToDate() {
        interactive.out.println "Checking whether local code is up to date"
        return true
    }

    void commit(String msg) {
        interactive.out.println "Committing current changes - commit message is: $msg"
    }

    void tag(String label, String msg) {
        interactive.out.println "Tagging the source code with label '$label' - commit message is: $msg"
    }

    void synchronize() {
        interactive.out.println "Local code synchronized with remote repository"
    }
}
