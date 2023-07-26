package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Date;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author Lucy Yang
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The staging area directory. */
    public static final File STAGING_ADD = join(GITLET_DIR, "staging_add");
    public static final File STAGING_RM = join(GITLET_DIR, "staging_rm");
    public static final File REPOSITORY = join(GITLET_DIR, "repository");
    public static final File BLOB_LIBRARY = join(GITLET_DIR, "blobs");
    public static final TreeMap<String, String> ADD_AREA = new TreeMap<>();
    public static final TreeMap<String, String> REMOVE_AREA = new TreeMap<>();
    public static final TreeMap<String, Commit> REPO = new TreeMap<>();
    public static final TreeMap<String, String> BLOBS = new TreeMap<>();

    /**
     * initializes an empty .gitlet repository.
     */
    public static void initCommand() {
        if (GITLET_DIR.isDirectory()) {
            System.out.println("A Gitlet version-control system already exists"
                    + " in the current directory.");
        } else {
            GITLET_DIR.mkdir();
            Commit initialCommit = new Commit();
            REPO.put(initialCommit.getHead(), initialCommit);
            TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
            String masterBranch = "master";
            branches.put(masterBranch, Commit.getHead());
            writeContents(Commit.ACTIVE_BRANCH, masterBranch);
            writeObject(Commit.BRANCHES, branches);
            writeObject(REPOSITORY, REPO);
            writeObject(STAGING_ADD, ADD_AREA);
            writeObject(STAGING_RM, REMOVE_AREA);
            writeObject(BLOB_LIBRARY, BLOBS);
        }
    }

    public static void addCommand(String fileName) {
        File tempFile = join(CWD, fileName);
        if (tempFile.exists()) {
            TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
            TreeMap<String, String> remove = readObject(STAGING_RM, TreeMap.class);
            TreeMap<String, String> blob = readObject(BLOB_LIBRARY, TreeMap.class);
            TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
            String blobHash = sha1(serialize(readContentsAsString(tempFile)));
            Commit curr = repo.get(Commit.getHead());
            TreeMap<String, String> commitLib = curr.getCommitLibrary();
            if (remove.containsKey(fileName)) {
                remove.remove(fileName);
                writeObject(STAGING_RM, remove);
            }
            if (commitLib.containsKey(fileName) && commitLib.get(fileName).equals(blobHash)) {
                if (add.containsKey(fileName) && add.get(fileName).equals(blobHash)) {
                    add.remove(fileName);
                    writeObject(STAGING_ADD, add);
                }
            } else {
                add.put(fileName, blobHash);
                blob.put(blobHash, readContentsAsString(tempFile));
                writeObject(BLOB_LIBRARY, blob);
                writeObject(STAGING_ADD, add);
            }
        } else {
            System.out.println("File does not exist.");
        }
    }

    public static void commitCommand(String message, String uid1, String uid2) {
        TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        TreeMap<String, Commit> remove = readObject(STAGING_RM, TreeMap.class);
        TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
        Date date = new Date();
        if (message.isBlank()) {
            System.out.println("Please enter a commit message.");
        } else if (add.isEmpty() && remove.isEmpty()) {
            System.out.println("No changes added to the commit.");
        } else {
            Commit newCommit;
            if (uid1 == null && uid2 == null) {
                newCommit = new Commit(message, date);
            } else {
                newCommit = new Commit(message, date, uid1, uid2);
            }
            TreeMap<String, String> newCommitLibrary = newCommit.getCommitLibrary();
            Commit newCommitParent = repo.get(newCommit.getParent());
            newCommitLibrary.putAll(newCommitParent.getCommitLibrary());
            newCommitLibrary.putAll(add);
            for (String keyVal : remove.keySet()) {
                if (newCommitLibrary.containsKey(keyVal)) {
                    newCommitLibrary.remove(keyVal);
                }
            }
            repo.put(newCommit.getHead(), newCommit);
            //updates the branch's head
            String activeBranch = readContentsAsString(Commit.ACTIVE_BRANCH);
            branches.put(activeBranch, Commit.getHead());
            writeObject(Commit.BRANCHES, branches);
            add.clear();
            remove.clear();
            writeObject(REPOSITORY, repo);
            writeObject(STAGING_ADD, add);
            writeObject(STAGING_RM, remove);
        }
    }

    /**
     *
     * ^^handled in the commit command
     * @param fileName
     */
    public static void rmCommand(String fileName) {
        File tempFile = join(CWD, fileName);
        TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
        TreeMap<String, String> remove = readObject(STAGING_RM, TreeMap.class);
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        Commit curr = repo.get(Commit.getHead());
        TreeMap<String, String> commitLib = curr.getCommitLibrary();
        String blobHash = sha1(serialize(tempFile));
        if (!commitLib.containsKey(fileName) && !add.containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
        } else {
            if (commitLib.containsKey(fileName)) {
                remove.put(fileName, blobHash);
                writeObject(STAGING_RM, remove);
                if (tempFile.exists()) {
                    restrictedDelete(tempFile);
                }
            }
            if (add.containsKey(fileName)) {
                add.remove(fileName);
                writeObject(STAGING_ADD, add);
            }
        }
    }


    public static void logCommand() {
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        Commit curr = repo.get(Commit.getHead());
        while (curr != null) {
            System.out.println("===");
            System.out.println("commit " + curr.getUID());
            if (curr.aResultOfMerge()) {
                String parent1 = curr.getParentList().get(0);
                String parent2 = curr.getParentList().get(1);
                System.out.println("Merge: " + parent1.substring(0, 7) + " "
                        + parent2.substring(0, 7));
            }
            System.out.println("Date: " + curr.getDate());
            System.out.println(curr.getMessage());
            System.out.println();
            if (curr.getParent() != null) {
                curr = repo.get(curr.getParent());
            } else {
                curr = null;
            }
        }
    }

    public static void globalLog() {
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        for (String key: repo.keySet()) {
            Commit curr = repo.get(key);
            System.out.println("===");
            System.out.println("commit " + curr.getUID());
            System.out.println("Date: " + curr.getDate());
            System.out.println(curr.getMessage());
            System.out.println();
        }
    }

    public static void findCommand(String message) {
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        boolean found = false;
        for (String key: repo.keySet()) {
            if (repo.get(key).getMessage().equals(message)) {
                System.out.println(key);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void statusCommand() {
        if (GITLET_DIR.exists()) {
            TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
            TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
            TreeMap<String, String> remove = readObject(STAGING_RM, TreeMap.class);
            TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
            Commit curr = repo.get(readContentsAsString(Commit.HEAD));
            TreeMap<String, String> currLib = curr.getCommitLibrary();
            System.out.println("=== Branches ===");
            for (String branch : branches.keySet()) {
                if (branch.equals(readContentsAsString(Commit.ACTIVE_BRANCH))) {
                    System.out.println("*" + branch);
                } else {
                    System.out.println(branch);
                }
            }
            System.out.println();
            System.out.println("=== Staged Files ===");
            for (String fileName : add.keySet()) {
                System.out.println(fileName);
            }
            System.out.println();
            System.out.println("=== Removed Files ===");
            for (String file : remove.keySet()) {
                System.out.print(file);
            }
            System.out.println();
            System.out.println("=== Modifications Not Staged For Commit ===");
            /*List<String> cwdFiles = plainFilenamesIn(CWD);
            for (String fileName: currLib.keySet()) {
                if (cwdFiles.contains(fileName)) {
                    File tempFile = join(CWD, fileName);
                    String blobHash = sha1(serialize(readContentsAsString(tempFile)));
                    if (!currLib.get(fileName).equals(blobHash)) {
                        System.out.println(fileName + " (modified)");
                    }
                } else {
                    if (remove.containsKey(fileName)) {
                        System.out.println(fileName + " (deleted)");
                    }
                }
            }*/
            for (String fileName: modifiedButNotStaged().keySet()) {
                System.out.println(fileName);
            }
            System.out.println();
            System.out.println("=== Untracked Files ===");
            for (String fileName: untracked().keySet()) {
                System.out.println(fileName);
            }
            System.out.println();
        } else {
            System.out.println("Not in an initialized Gitlet directory.");
        }
    }

    public static TreeMap<String, String> untracked() {
        List<String> filesInWorkingDir = plainFilenamesIn(CWD);
        TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        Commit curr = repo.get(readContentsAsString(Commit.HEAD));
        TreeMap<String, String> currLib = curr.getCommitLibrary();
        TreeMap<String, String>  filesForReturn = new TreeMap<>();
        for (String fileName: filesInWorkingDir) {
            if (!add.containsKey(fileName) && !currLib.containsKey(fileName)) {
                filesForReturn.put(fileName, "random");
            }
        }
        return filesForReturn;
    }


    /**public static TreeMap<String, String> modifiedButNotStaged() {
        TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
        TreeMap<String, String> remove = readObject(STAGING_RM, TreeMap.class);
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        Commit curr = repo.get(readContentsAsString(Commit.HEAD));
        TreeMap<String, String> currLib = curr.getCommitLibrary();
        List<String> filesInWorkingDir = plainFilenamesIn(CWD);
        TreeMap<String, String> allFiles = plainFilenamesIn(CWD);
        TreeMap<String, String> filesModified = new TreeMap<>();
        for (String file: currLib.keySet()) {
            allFiles.put(file, "none");
        }
        for (String fileName: allFiles) {
            if (filesInWorkingDir.contains(fileName)) {
                File tempFile = join(CWD, fileName);
                String blobHash = sha1(serialize(readContentsAsString(tempFile)));
                if (currLib.containsKey(fileName)) {
                    if (!currLib.get(fileName).equals(blobHash)) {
                        filesModified.put(fileName + " (modified)", blobHash);
                    }
                } else {
                    if (add.containsKey(fileName)) {
                        if(!add.get(fileName).equals(blobHash)) {
                            filesModified.put(fileName + " (modified)", blobHash);
                        }
                    }
                }
            } else {
                if (!currLib.containsKey(fileName)) {
                    if (add.containsKey(fileName)) {
                        filesModified.put(fileName + " (deleted)", "none");
                    }
                } else {
                    if (!remove.containsKey(fileName)) {
                        filesModified.put(fileName + " (deleted)", "none");
                    }
                }
            }
        }
        return filesModified;
    } */

    public static TreeMap<String, String> modifiedButNotStaged() {
        TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
        TreeMap<String, String> remove = readObject(STAGING_RM, TreeMap.class);
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        Commit curr = repo.get(readContentsAsString(Commit.HEAD));
        TreeMap<String, String> currLib = curr.getCommitLibrary();
        List<String> filesInWorkingDir = plainFilenamesIn(CWD);
        /**for (String file: filesInWorkingDir) {
            System.out.println(file);
        } */
        TreeMap<String, String> filesModified = new TreeMap<>();
        for (String fileName: currLib.keySet()) {
            if (filesInWorkingDir.contains(fileName)) {
                File tempFile = join(CWD, fileName);
                String blobHash = sha1(serialize(readContentsAsString(tempFile)));
                if (!currLib.get(fileName).equals(blobHash)) {
                    filesModified.put(fileName + " (modified)", blobHash);
                }
            } else {
                if (!remove.containsKey(fileName) && currLib.containsKey(fileName)) {
                    filesModified.put(fileName + " (deleted)", "none");
                }
            }
        }
        for (String file: add.keySet()) {
            if (filesInWorkingDir.contains(file)) {
                File tempFile = join(CWD, file);
                String blobHash = sha1(serialize(readContentsAsString(tempFile)));
                if (!add.get(file).equals(blobHash) && !currLib.containsKey(file)) {
                    filesModified.put(file + " (modified)", blobHash);
                }
            } else {
                filesModified.put(file + " (deleted)", "none");
            }
        }
        return filesModified;
    }

    public static void checkoutCommand(String fileName) {
        File tempFile = join(CWD, fileName);
        if (tempFile.exists()) {
            TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
            TreeMap<String, String> blobs = readObject(BLOB_LIBRARY, TreeMap.class);
            Commit curr = repo.get(Commit.getHead());
            TreeMap<String, String> currentVersion = curr.getCommitLibrary();
            String blobHash = currentVersion.get(fileName);
            String contents = blobs.get(blobHash);
            writeContents(tempFile, contents);
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    public static void checkoutCommand(String commitID, String fileName) {
        File tempFile = join(CWD, fileName);
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        TreeMap<String, String> blobs = readObject(BLOB_LIBRARY, TreeMap.class);
        String key = shortenedUID(commitID);
        //check if tempfile exists
        if (key != null) {
            Commit correctCommit = repo.get(key);
            if (correctCommit.getCommitLibrary().containsKey(fileName)) {
                TreeMap<String, String> commitFiles = correctCommit.getCommitLibrary();
                String blobHash = commitFiles.get(fileName);
                String contents = blobs.get(blobHash);
                writeContents(tempFile, contents);
                //readContentsAsString(tempFile);
            } else {
                System.out.println("File does not exist in that commit.");
            }
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    /** moves the pointer for the head command to the commit mapped to branchName in branches
     */
    public static void checkoutBranchCommand(String branchName) {
        TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
        TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
        TreeMap<String, String> remove = readObject(STAGING_RM, TreeMap.class);
        if (branches.containsKey(branchName)) {
            List<String> files = plainFilenamesIn(CWD);
            String activeBranch = readContentsAsString(Commit.ACTIVE_BRANCH);
            if (activeBranch.equals(branchName)) {
                System.out.println("No need to checkout the current branch.");
            } else {
                TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
                Commit active = repo.get(branches.get(activeBranch));
                Commit checkingOut = repo.get(branches.get(branchName));
                TreeMap<String, String> activeLibrary = active.getCommitLibrary();
                TreeMap<String, String> checkingOutLibrary = checkingOut.getCommitLibrary();
                for (String file : files) {
                    if (!activeLibrary.containsKey(file) && checkingOutLibrary.containsKey(file)
                            && !add.containsKey(file)) {
                        System.out.println("There is an untracked file in the way; delete it, "
                                + "or add and commit it first.");
                        System.exit(0);
                    }
                }
                TreeMap<String, String> blobs = readObject(BLOB_LIBRARY, TreeMap.class);
                for (String fileNames : activeLibrary.keySet()) {
                    File temp = join(CWD, fileNames);
                    if (!checkingOutLibrary.containsKey(fileNames)) {
                        restrictedDelete(temp);
                    }
                }
                for (String fileName : checkingOutLibrary.keySet()) {
                    File file = join(CWD, fileName);
                    String blobHash = checkingOutLibrary.get(fileName);
                    String contents = blobs.get(blobHash);
                    writeContents(file, contents);
                }
                remove.clear(); //clear staging area
                add.clear();
                writeObject(STAGING_ADD, add);
                writeObject(STAGING_RM, remove);
                writeContents(Commit.ACTIVE_BRANCH, branchName);
                writeContents(Commit.HEAD, branches.get(branchName));
            }
        } else {
            System.out.println("No such branch exists.");
        }
    }

    public static void branchCommand(String branchName) {
        TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
        if (!branches.containsKey(branchName)) {
            branches.put(branchName, Commit.getHead());
            writeObject(Commit.BRANCHES, branches);
        } else {
            System.out.println("A branch with that name already exists.");
        }
    }

    public static void rmBranch(String branchName) {
        TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (branchName.equals(readContentsAsString(Commit.ACTIVE_BRANCH))) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        } else {
            branches.remove(branchName);
            writeObject(Commit.BRANCHES, branches);
        }
    }

    public static void resetCommand(String commitID) {
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        String key = shortenedUID(commitID);
        if (key == null) {
            System.out.println("No commit with that id exists.");
        } else {
            if (repo.containsKey(key)) {
                TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
                TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
                TreeMap<String, String> remove = readObject(STAGING_RM, TreeMap.class);
                Commit desired = repo.get(key);
                Commit head = repo.get(Commit.getHead());
                TreeMap<String, String> desiredLib = desired.getCommitLibrary();
                TreeMap<String, String> headLib = head.getCommitLibrary();
                TreeMap<String, String> blobs = readObject(BLOB_LIBRARY, TreeMap.class);
                List<String> cwdFiles = plainFilenamesIn(CWD);
                for (String cwdFile : cwdFiles) {
                    File cwf = join(CWD, cwdFile);
                    if (!headLib.containsKey(cwdFile) && desiredLib.containsKey(cwdFile)
                            && !add.containsKey(cwdFile)) {
                        System.out.println("There is an untracked file in the way; "
                                + "delete it, or add and commit it first.");
                        System.exit(0);
                    } else {
                        if (!desiredLib.containsKey(cwdFile)) {
                            rmCommand(cwdFile); //JUST ADDED
                            restrictedDelete(cwf);
                        }
                    }
                }
                for (String file : desiredLib.keySet()) {
                    checkoutCommand(key, file);
                    /**File temp = join(CWD, file);
                     String blobHash = desiredLib.get(file);
                     String contents = blobs.get(blobHash);
                     if (temp.exists()) {
                     writeContents(temp, contents);
                     } else {
                     writeContents(temp, contents);
                     addCommand(file);
                     } */
                }
                String activeBranch = readContentsAsString(Commit.ACTIVE_BRANCH);
                branches.put(activeBranch, key);
                add.clear();
                remove.clear();
                writeObject(Commit.BRANCHES, branches);
                writeObject(STAGING_RM, remove);
                writeObject(STAGING_ADD, add);
                writeContents(Commit.HEAD, key);
            }
        }
    }

    private static String shortenedUID(String commitID) {
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        if (repo.containsKey(commitID)) {
            return commitID;
        } else {
            for (String key: repo.keySet()) {
                if (key.startsWith(commitID)) {
                    return key;
                }
            }
            return null;
        }
    }

    /**
     * @param branchName
     */
    public static void mergeCommand(String branchName) {
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
        TreeMap<String, String> blobs = readObject(BLOB_LIBRARY, TreeMap.class);
        String activeBranch = readContentsAsString(Commit.ACTIVE_BRANCH);
        Commit currentBranchCommit = repo.get(branches.get(activeBranch));
        if (!uncommittedChanges()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (activeBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        if (untrackedFile(branchName)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            System.exit(0);
        }
        Commit givenBranchCommit = repo.get(branches.get(branchName));
        Commit splitPointCommit = findSplitPoint(branchName, activeBranch);
        if (splitPointCommit.getUID().equals(givenBranchCommit.getUID())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPointCommit.getUID().equals(currentBranchCommit.getUID())) {
            checkoutBranchCommand(branchName);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        ArrayList<Commit> allCommits = new ArrayList<>();
        allCommits.add(currentBranchCommit);
        allCommits.add(givenBranchCommit);
        allCommits.add(splitPointCommit);
        HashSet<String> allFiles = generateFileSet(allCommits);
        for (String fileName: allFiles) {
            File tempFile = join(CWD, fileName);
            int whichRuleFits = whichRule(fileName, branchName, splitPointCommit);
            if (whichRuleFits == 1) {
                checkoutCommand(givenBranchCommit.getUID(), fileName);
                addCommand(fileName);
            } else if (whichRuleFits == 2 || whichRuleFits == 3 || whichRuleFits == 4
                            || whichRuleFits == 7) {
                continue;
            } else if (whichRuleFits == 5) {
                Commit givenCommit = repo.get(givenBranchCommit.getUID());
                TreeMap<String, String> givenCommitLib = givenCommit.getCommitLibrary();
                String blobHash = givenCommitLib.get(fileName);
                String contents = blobs.get(blobHash);
                writeContents(tempFile, contents);
                addCommand(fileName);
            } else if (whichRuleFits == 6) {
                rmCommand(fileName);
            } else {
                System.out.println("Encountered a merge conflict.");
                String contentsOfCurrentBranch = "";
                String contentsOfGivenBranch = "";
                if (currentBranchCommit.getCommitLibrary().containsKey(fileName)) {
                    contentsOfCurrentBranch = blobs.get(
                            currentBranchCommit.getCommitLibrary().get(fileName));
                }
                if (givenBranchCommit.getCommitLibrary().containsKey(fileName)) {
                    contentsOfGivenBranch = blobs.get(
                            givenBranchCommit.getCommitLibrary().get(fileName));
                }
                String contents = createConflictContents(contentsOfCurrentBranch,
                        contentsOfGivenBranch);
                writeContents(tempFile, contents);
                addCommand(fileName);
            }
        }
        String uiD1 = branches.get(activeBranch);
        String uiD2 = branches.get(branchName);
        String message = "Merged " + branchName + " into " + activeBranch + ".";
        commitCommand(message, uiD1, uiD2);
    }

    public static String createConflictContents(String contentsOfCurrentBranch,
                                                String contentsOfGivenBranch) {
        String contents = "<<<<<<< HEAD\n" + contentsOfCurrentBranch + "=======\n"
                + contentsOfGivenBranch + ">>>>>>>\n";
        return contents;
    }

    public static HashSet<String> generateFileSet(ArrayList<Commit> commits) {
        HashSet<String> allFiles = new HashSet<>();
        for (Commit curr: commits) {
            TreeMap<String, String> currLib = curr.getCommitLibrary();
            for (String key: currLib.keySet()) {
                if (!allFiles.contains(key)) {
                    allFiles.add(key);
                }
            }
        }
        return allFiles;
    }

    public static Integer whichRule(String fileName, String branchName, Commit splitPoint) {
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
        TreeMap<String, String> blobs = readObject(BLOB_LIBRARY, TreeMap.class);
        Commit currentBranch = repo.get(readContentsAsString(Commit.HEAD));
        Commit givenBranch = repo.get(branches.get(branchName));
        TreeMap<String, String> currentBranchLib = currentBranch.getCommitLibrary();
        TreeMap<String, String> givenBranchLib = givenBranch.getCommitLibrary();
        TreeMap<String, String> splitPointLib = splitPoint.getCommitLibrary();
        String splitPointBlobHash = "removed";
        String currentBranchBlobHash = "removed";
        String givenBranchBlobHash = "removed";
        if (splitPointLib.containsKey(fileName)) {
            splitPointBlobHash = splitPointLib.get(fileName);
        }
        if (currentBranchLib.containsKey(fileName)) {
            currentBranchBlobHash = currentBranchLib.get(fileName);
        }
        if (givenBranchLib.containsKey(fileName)) {
            givenBranchBlobHash = givenBranchLib.get(fileName);
        }
        if ((splitPointLib.containsKey(fileName) && givenBranchLib.containsKey(fileName)
                && currentBranchLib.containsKey(fileName))) {
            if (!splitPointBlobHash.equals(givenBranchBlobHash)
                    && splitPointBlobHash.equals(currentBranchBlobHash)) {
                return 1;
            }
            if (!splitPointBlobHash.equals(currentBranchBlobHash)
                    && splitPointBlobHash.equals(givenBranchBlobHash)) {
                return 2;
            }
        }
        if ((splitPointLib.containsKey(fileName)
                && !splitPointBlobHash.equals(givenBranchBlobHash)
                && !splitPointBlobHash.equals(currentBranchBlobHash))
                && currentBranchBlobHash.equals(givenBranchBlobHash)) {
            return 3;
            //if split point and given branch do not contain, but current branch does
        }
        if (!splitPointLib.containsKey(fileName) && !givenBranchLib.containsKey(fileName)
                && currentBranchLib.containsKey(fileName)) {
            return 4;
            // if split point and current branch don't contain file, and it's only in given branch
        }
        if (!splitPointLib.containsKey(fileName) && !currentBranchLib.containsKey(fileName)
                && givenBranchLib.containsKey(fileName)) {
            return 5;
            // if file present at split point, unmodified in current branch, absent in given branch
        }
        if (splitPointLib.containsKey(fileName) && currentBranchBlobHash.equals(splitPointBlobHash)
                && !givenBranchLib.containsKey(fileName)) {
            return 6;
            //if file present at split, unmodified in given, absent in given
        }
        if (splitPointLib.containsKey(fileName) && givenBranchBlobHash.equals(splitPointBlobHash)
                && !currentBranchLib.containsKey(fileName)) {
            return 7;
            // if files modified in different ways
        }
        if (!givenBranchBlobHash.equals(currentBranchBlobHash)) {
            return 8;
        }
        return null;
    }

    /**
     *Find the split point between two branches
     */
    public static Commit findSplitPoint(String givenBranch, String currentBranch) {
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
        HashSet<String> currBranchAncestors = new HashSet<>();
        Commit currentBranchCommit = repo.get(branches.get(currentBranch));
        Commit givenBranchCommit = repo.get(branches.get(givenBranch));
        //perform first bfs
        LinkedList<Commit> branchAncestorQueue = new LinkedList<>();
        branchAncestorQueue.addLast(currentBranchCommit);
        while (!branchAncestorQueue.isEmpty()) {
            //pop the first element
            Commit v = branchAncestorQueue.removeFirst();
            //visit the node
            currBranchAncestors.add(v.getUID());
            //add parents to queue
            if (v.getParentList() != null) {
                for (String parent : v.getParentList()) {
                    branchAncestorQueue.addLast(repo.get(parent));
                }
            }
        }
        //perform second bfs
        LinkedList<Commit> givenBranchQueue = new LinkedList<>();
        givenBranchQueue.addLast(givenBranchCommit);
        while (!givenBranchQueue.isEmpty()) {
            //pop the first element
            Commit z = givenBranchQueue.removeFirst();
            //visit node
            if (currBranchAncestors.contains(z.getUID())) {
                return z;
            } else {
                if (z.getParentList() != null) {
                    for (String parent : z.getParentList()) {
                        givenBranchQueue.addLast(repo.get(parent));
                    }
                }
            }
        }
        //if there is no split point
        return null;
    }

    //return ture if there are uncommitted changes
    public static boolean uncommittedChanges() {
        TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
        TreeMap<String, String> remove = readObject(STAGING_RM, TreeMap.class);
        return add.isEmpty() && remove.isEmpty();
    }

    //return true if there is an untracked file
    public static boolean untrackedFile(String branchName) {
        TreeMap<String, String> add = readObject(STAGING_ADD, TreeMap.class);
        TreeMap<String, Commit> repo = readObject(REPOSITORY, TreeMap.class);
        TreeMap<String, String> branches = readObject(Commit.BRANCHES, TreeMap.class);
        Commit currCommit = repo.get(readContentsAsString(Commit.HEAD));
        Commit checkingOutCommit = repo.get(branches.get(branchName));
        TreeMap<String, String> checkingOutCommitLib = checkingOutCommit.getCommitLibrary();
        TreeMap<String, String> currCommitLib = currCommit.getCommitLibrary();
        List<String> cwdFiles = plainFilenamesIn(CWD);
        for (String file: cwdFiles) {
            File tempFile = join(CWD, file);
            if (!currCommitLib.containsKey(file) && !add.containsKey(file)
                    && checkingOutCommitLib.containsKey(file)) {
                return true;
            }
        } return false;
    }
}
