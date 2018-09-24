package gitlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;

/** An object that reads and interperets commands from an input source.
 * @author Jeff Xiang
 * (with inspiration from the CommandInterpreter class in Proj1)
 */

public class CommandInterpreter {

    /** A new CommandInterpreter object.
     * @param inp input string array */
    CommandInterpreter(String[] inp) {
        _input = inp;
    }

    /** Parse and execute one statement from the token stream. */
    void statement() {
        if (_input.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        boolean correctops = true;
        if (!_input[0].equals("init")) {
            if (!new File(".gitlet").exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                return;
            }
        }
        switch (_input[0]) {
        case "init":
            correctops = initStatement();
            break;
        case "add":
            correctops = addStatement();
            break;
        case "commit":
            correctops = commitStatement();
            break;
        case "rm":
            correctops = rmStatement();
            break;
        case "log":
            correctops = logStatement();
            break;
        case "global-log":
            correctops = globallogStatement();
            break;
        case "find":
            correctops = findStatement();
            break;
        case "status":
            correctops = statusStatement();
            break;
        case "checkout":
            correctops = checkoutStatement();
            break;
        case "branch":
            correctops = branchStatement();
            break;
        case "rm-branch":
            correctops = rmbranchStatement();
            break;
        case "reset":
            correctops = resetStatement();
            break;
        case "merge":
            correctops = mergeStatement();
            break;
        default:
            System.out.println("No command with that name exists.");
            break;
        }
        if (!correctops) {
            System.out.println("Incorrect operands.");
        }
    }

    /** Execute an init statement.
     * @return true if operands are correct. */
    boolean initStatement() {
        if (_input.length != 1) {
            return false;
        }
        File f = new File(".gitlet");
        boolean success = f.mkdir();
        if (!success) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            return true;
        } else {
            File serializeddir = new File(".gitlet/serialized");
            File stageddir = new File(".gitlet/staged");
            serializeddir.mkdir();
            stageddir.mkdir();
            Commit initcommit = new Commit();
            CommitTree commitTree = new CommitTree(initcommit);
            initcommit.serialize();
            commitTree.serialize();
            return true;
        }
    }

    /** Execute an add statement.
     * @return true if operands are correct. */
    boolean addStatement() {
        if (_input.length != 2) {
            return false;
        }
        String filename = _input[1];
        File toadd = new File(filename);
        if (!toadd.exists()) {
            System.out.println("File does not exist.");
            return true;
        }
        byte[] toaddcontents = Utils.readContents(toadd);
        CommitTree commitTree = Utils.readObject(
                new File(Utils.SERIALIZED + "CT"), CommitTree.class);
        Commit headcommit = commitTree.getHeadCommit();
        if (headcommit.blobExists(filename)) {
            Blob commited = headcommit.getBlob(filename);
            byte[] commitedcontents = commited.getContents();
            if (Arrays.equals(toaddcontents, commitedcontents)) {
                new File(Utils.STAGED + filename).delete();
                return true;
            }
            if (commitTree.getRmMarked().contains(filename)) {
                commitTree.removeRmMarked(filename);
                commitTree.serialize();
            }
        }
        File tostage = new File(Utils.STAGED + filename);
        Utils.writeContents(tostage, toaddcontents);
        return true;
    }

    /** Execute a commit statement.
     * @return true if operands are correct. */
    boolean commitStatement() {
        if (_input.length > 2) {
            return false;
        }
        if (_input.length == 1
                || (_input.length == 2 && _input[1].equals(""))) {
            System.out.println("Please enter a commit message.");
            return true;
        }
        String message = _input[1];
        List<String> files = Arrays.asList(new File(Utils.STAGED).list());
        CommitTree commitTree = Utils.getCommitTree();
        if ((files.size() == 0)
                && commitTree.getRmMarked().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return true;
        }
        Commit c = new Commit(message, files);
        commitTree.setHead(c.getSHA());
        c.initiate(c.getFilenames());
        commitTree.addCommit(c.getSHA());
        commitTree.clearrmMarked();
        commitTree.serialize();
        for (String filename: files) {
            File todelete = new File(Utils.STAGED + filename);
            todelete.delete();
        }
        return true;
    }

    /** Execute an rm statement.
     * @return true if operands are correct. */
    boolean rmStatement() {
        if (_input.length != 2) {
            return false;
        }
        String filename = _input[1];
        File torm = new File(Utils.STAGED + filename);
        CommitTree commitTree = Utils.getCommitTree();
        Commit headcommit = commitTree.getHeadCommit();
        boolean tormexists = torm.exists();
        if (!tormexists && !headcommit.blobExists(filename)) {
            System.out.println("No reason to remove the file.");
            return true;
        }
        if (tormexists) {
            torm.delete();
        }
        if (headcommit.blobExists(filename)) {
            commitTree.addRmMarked(filename);
            commitTree.serialize();
            if (new File(filename).exists()) {
                Utils.restrictedDelete(new File(filename));
                commitTree.addRemoved(filename);
                commitTree.serialize();
            }
        }
        return true;
    }

    /** Execute a log statement.
     * @return true if operands are correct. */
    boolean logStatement() {
        if (_input.length != 1) {
            return false;
        }
        CommitTree commitTree = Utils.getCommitTree();
        Commit currcommit = commitTree.getHeadCommit();
        while (currcommit != null) {
            System.out.println("===");
            System.out.println("commit " + currcommit.getSHA());
            if (currcommit.getParent2SHA() != null) {
                System.out.println("Merge: "
                        + currcommit.getParentSHA().substring(0, 7) + " "
                        + currcommit.getParent2SHA().substring(0, 7));
            }
            System.out.println("Date: " + currcommit.getDate());
            System.out.println(currcommit.getMessage());
            System.out.println("");
            if (currcommit.getParentSHA() == null) {
                break;
            }
            currcommit = currcommit.getParentCommit();
        }
        return true;
    }

    /** Execute a global-log statement.
     * @return true if operands are correct. */
    boolean globallogStatement() {
        if (_input.length != 1) {
            return false;
        }
        ArrayList<String> commits = Utils.getCommitTree().getCommits();
        for (String commitSHA: commits) {
            Commit currcommit = Utils.readObject(
                    new File(Utils.SERIALIZED + commitSHA), Commit.class);
            System.out.println("===");
            System.out.println("commit " + currcommit.getSHA());
            if (currcommit.getParent2SHA() != null) {
                System.out.println("Merge: "
                        + currcommit.getParentSHA().substring(0, 6) + " "
                        + currcommit.getParent2SHA().substring(0, 6));
            }
            System.out.println("Date: " + currcommit.getDate());
            System.out.println(currcommit.getMessage());
            System.out.println("");
        }
        return true;
    }

    /** Execute a find statement.
     * @return true if operands are correct. */
    boolean findStatement() {
        if (_input.length != 2) {
            return false;
        }
        String commitmessage = _input[1];
        ArrayList<String> commits = Utils.getCommitTree().getCommits();
        int found = 0;
        for (String commitSHA: commits) {
            Commit currcommit = Utils.readObject(
                    new File(Utils.SERIALIZED + commitSHA), Commit.class);
            if (currcommit.getMessage().equals(commitmessage)) {
                System.out.println(currcommit.getSHA());
                found++;
            }
        }
        if (found == 0) {
            System.out.println("Found no commit with that message.");
        }
        return true;
    }

    /** Execute a status statement.
     * @return true if operands are correct. */
    boolean statusStatement() {
        if (_input.length != 1) {
            return false;
        }
        CommitTree commitTree = Utils.getCommitTree();
        ArrayList<String> branches = commitTree.getBranches();
        Collections.sort(branches);
        System.out.println("=== Branches ===");
        for (String branch: branches) {
            if (!branch.equals(commitTree.getHeadName())) {
                System.out.println(branch);
            } else {
                System.out.println("*" + commitTree.getHeadName());
            }
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        String[] filenames = new File(Utils.STAGED).list();
        if (filenames != null) {
            Arrays.sort(filenames);
        }
        for (String filename: filenames) {
            System.out.println(filename);
        }
        System.out.println("");
        System.out.println("=== Removed Files ===");
        ArrayList<String> removed = commitTree.getRemoved();
        Collections.sort(removed);
        for (String removedfilename: removed) {
            if (!new File(removedfilename).exists()) {
                System.out.println(removedfilename);
            }
        }
        System.out.println("");
        System.out.println("=== Modifications Not Staged For Commit ===");
        removedfiles();
        System.out.println("=== Untracked Files ===");
        String[] filesinworkingdir = new File(".").list();
        Commit headcommit = commitTree.getHeadCommit();
        ArrayList<String> untracked = new ArrayList<>();
        for (String filename: filesinworkingdir) {
            boolean staged = new File(Utils.STAGED + filename).exists();
            boolean tracked = headcommit.blobExists(filename);
            if (!staged && !tracked) {
                untracked.add(filename);
            }
        }
        Collections.sort(untracked);
        for (String untrackedname: untracked) {
            if (!new File(untrackedname).isDirectory()) {
                System.out.println(untrackedname);
            }
        }
        System.out.println("");
        return true;
    }

    /** List out removed files. */
    void removedfiles() {
        ArrayList<String> modnotstaged = modnotstaged();
        CommitTree commitTree = Utils.getCommitTree();
        List<String> headfiles = commitTree.getHeadCommit().getFilenames();
        for (String headfile: headfiles) {
            boolean stagedforrm = commitTree.getRmMarked().contains(headfile);
            if (!stagedforrm
                    && !new File(headfile).exists()) {
                modnotstaged.add(headfile);
            }
        }
        Collections.sort(modnotstaged);
        for (String modnotstagedfile: modnotstaged) {
            boolean deleted = !new File(modnotstagedfile).exists();
            if (deleted) {
                System.out.println(modnotstagedfile + " (deleted)");
            } else {
                System.out.println(modnotstagedfile + " (modified)");
            }
        }
        System.out.println("");
    }

    /** Helper method for status. Returns an ArrayList
     * of filenames that are modified but not
     * staged for commit, unordered. */
    private ArrayList<String> modnotstaged() {
        ArrayList<String> modnotstaged = new ArrayList<>();
        String[] filesinworkingdir = new File(".").list();
        CommitTree commitTree = Utils.getCommitTree();
        Commit headcommit = commitTree.getHeadCommit();
        for (String filename: filesinworkingdir) {
            boolean cond1, cond2, cond3, cond4;
            cond1 = cond2 = cond3 = cond4 = false;
            boolean workingfileexists = new File(filename).exists();
            boolean tracked = headcommit.blobExists(filename);
            boolean staged = new File(Utils.STAGED + filename).exists();
            boolean stagedforremoval = commitTree.getRmMarked()
                    .contains(filename);
            if (workingfileexists) {
                if (tracked) {
                    boolean changed = !headcommit.sameContents(filename);
                    if (changed && !staged) {
                        cond1 = true;
                    }
                }
                if (staged) {
                    byte[] stagedcontents = Utils.readContents(
                            new File(Utils.STAGED + filename));
                    byte[] workingcontents =
                            Utils.readContents(new File(filename));
                    if (!Arrays.equals(stagedcontents, workingcontents)) {
                        cond2 = true;
                    }
                }
            } else {
                if (staged) {
                    cond3 = true;
                }
                if (!stagedforremoval && tracked) {
                    cond4 = true;
                }
            }
            if (cond1 || cond2 || cond3 || cond4) {
                modnotstaged.add(filename);
            }
        }
        return modnotstaged;
    }

    /** Case 1 of checkout.
     * @param commitTree commit tree */
    void checkoutcase1(CommitTree commitTree) {
        String filename = _input[2];
        Commit headcommit = commitTree.getHeadCommit();
        if (!headcommit.blobExists(filename)) {
            System.out.println("File does not exist in that commit.");
        } else {
            Blob commitedblob = headcommit.getBlob(filename);
            byte[] commitedcontents = commitedblob.getContents();
            Utils.writeContents(new File(filename), commitedcontents);
        }
    }

    /** Case 2 of checkout. */
    void checkoutcase2() {
        String commitid = _input[1];
        String filename = _input[3];
        for (String name: new File(Utils.SERIALIZED).list()) {
            if (!name.equals("CT") && name.contains(commitid)
                    || name.equals(commitid)) {
                Commit commit = Utils.readObject(
                        new File(Utils.SERIALIZED + name), Commit.class);
                if (!commit.blobExists(filename)) {
                    System.out.print("File does not exist in that commit.");
                    return;
                }
                Blob commitedblob = commit.getBlob(filename);
                byte[] commitedcontents = commitedblob.getContents();
                Utils.writeContents(new File(filename), commitedcontents);
                return;
            }
        }
        System.out.println("No commit with that id exists.");
    }

    /** Executes a checkout statement.
     * @return true if operands are correct. */
    boolean checkoutStatement() {
        CommitTree commitTree = Utils.getCommitTree();
        try {
            if (_input[1].equals("--")) {
                checkoutcase1(commitTree);
                return true;
            } else if (_input.length == 4 && _input[2].equals("--")) {
                checkoutcase2();
                return true;
            } else if (_input.length == 2) {
                String branchname = _input[1];
                if (!commitTree.getBranchMap().containsKey(branchname)) {
                    System.out.println("No such branch exists.");
                    return true;
                } else if (commitTree.getHeadBranchName().equals(branchname)) {
                    System.out.println(
                            "No need to checkout the current branch.");
                    return true;
                }
                Commit tocommit = commitTree.getHeadCommit(branchname);
                String[] workingfilelist = new File(".").list();
                ArrayList<String> notintocommit = new ArrayList<>();
                for (String workingfile : workingfilelist) {
                    if (untrackedChange(workingfile, tocommit)) {
                        return true;
                    }
                    if (!tocommit.blobExists(workingfile)) {
                        notintocommit.add(workingfile);
                    }
                }
                List<String> commitfiles = tocommit.getFilenames();
                for (String file : commitfiles) {
                    Blob b = tocommit.getBlob(file);
                    Utils.writeContents(new File(b.getName()), b.getContents());
                }
                for (String filenotincommit : notintocommit) {
                    if (!filenotincommit.equals("Makefile")
                            && !filenotincommit.equals("proj3.iml")) {
                        Utils.restrictedDelete(filenotincommit);
                    }
                }
                commitTree.setHeadBranchName(branchname);
                commitTree.setHead(tocommit.getSHA());
                commitTree.serialize();
                return true;
            } else {
                return false;
            }
        } catch (ArrayIndexOutOfBoundsException excp) {
            return false;
        }
    }

    /** Executes a branch statement.
     * @return true if operands are correct. */
    boolean branchStatement() {
        if (_input.length != 2) {
            return false;
        }
        String branchname = _input[1];
        CommitTree commitTree = Utils.getCommitTree();
        if (commitTree.getBranchMap().containsKey(branchname)) {
            System.out.println("A branch with that name already exists.");
            return true;
        }
        commitTree.addBranch(branchname);
        commitTree.serialize();
        return true;
    }

    /** Executes a rm-branch statement.
     * @return true if operands are correct. */
    boolean rmbranchStatement() {
        if (_input.length != 2) {
            return false;
        }
        String branchname = _input[1];
        CommitTree commitTree = Utils.getCommitTree();
        if (!commitTree.getBranchMap().containsKey(branchname)) {
            System.out.println("A branch with that name does not exist.");
        } else {
            if (commitTree.getHeadBranchName().equals(branchname)) {
                System.out.println("Cannot remove the current branch.");
            } else {
                commitTree.rmBranch(branchname);
                commitTree.serialize();
            }
        }
        return true;
    }

    /** Executes a reset statement.
     * @return true if operands are correct. */
    boolean resetStatement() {
        if (_input.length != 2) {
            return false;
        }
        String commitid = _input[1];
        CommitTree commitTree = Utils.getCommitTree();
        Commit currheadcommit = commitTree.getHeadCommit();
        Commit tocommit = commitTree.getCommit(commitid);
        if (tocommit == null) {
            return true;
        }
        String[] workingfilelist = new File(".").list();
        ArrayList<String> notintocommit = new ArrayList<>();
        if (workingfilelist != null) {
            for (String workingfile : workingfilelist) {
                boolean untrackedchange =
                        untrackedChange(workingfile, tocommit);
                if (untrackedchange) {
                    return true;
                }
                if (currheadcommit.blobExists(workingfile)
                        && !tocommit.blobExists(workingfile)) {
                    notintocommit.add(workingfile);
                }
            }
        }
        List<String> commitfiles = tocommit.getFilenames();
        for (String file: commitfiles) {
            Blob blob = tocommit.getBlob(file);
            Utils.writeContents(new File(blob.getName()), blob.getContents());
        }
        for (String filenotincommit: notintocommit) {
            if (!filenotincommit.equals("Makefile")
                    && !filenotincommit.equals("proj3.iml")) {
                Utils.restrictedDelete(filenotincommit);
            }
        }
        String[] stagedfiles = new File(Utils.STAGED).list();
        for (String filename: stagedfiles) {
            File todelete = new File(Utils.STAGED + filename);
            todelete.delete();
        }
        commitTree.setHead(tocommit.getSHA());
        commitTree.serialize();
        return true;
    }

    /** Returns true if the given file in the current working directory
     * has been changed and is untracked in the
     * current head and would be modified or deleted
     * by reverting to TOCOMMIT.
     * @param filename File name
     * @param tocommit commit object to revert to */
    boolean untrackedChange(String filename, Commit tocommit) {
        CommitTree commitTree = Utils.getCommitTree();
        Commit currheadcommit = commitTree.getHeadCommit();
        if (!currheadcommit.blobExists(filename)
                && tocommit.blobExists(filename)
                && !tocommit.sameContents(filename)) {
            if (!new File(filename).isDirectory()
                    && !filename.equals("Makefile")
                    && !filename.equals("proj3.iml")
                    && !filename.equals(".DS_Store")) {
                System.out.println(
                       "There is an untracked file in the way; "
                              + "delete it or add it first.");
                return true;
            }
        }
        return false;
    }

    /** Executes a merge statement.
     * @return true if operands are correct. */
    boolean mergeStatement() {
        if (_input.length != 2) {
            return false;
        }
        String givenbranch = _input[1];
        CommitTree commitTree = Utils.getCommitTree();
        String currbranch = commitTree.getHeadBranchName();
        String[] stagedfiles = new File(Utils.STAGED).list();
        if ((stagedfiles != null && stagedfiles.length != 0)
                || !commitTree.getRmMarked().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (!commitTree.getBranches().contains(givenbranch)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (givenbranch.equals(commitTree.getHeadBranchName())) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        Commit splitpoint = findSplitPoint(commitTree, givenbranch);
        if (splitpoint == null) {
            return true;
        }
        Commit currhead = commitTree.getHeadCommit();
        Commit givenhead = commitTree.getHeadCommit(givenbranch);
        List<String> givenfiles = givenhead.getFilenames();
        for (String givenfile: givenfiles) {
            boolean givenincurrdir = new File(givenfile).exists();
            if (givenincurrdir && untrackedChange(givenfile, givenhead)) {
                return true;
            }
            Blob givenblob = givenhead.getBlob(givenfile);
            boolean givenexistsinsplit = splitpoint.blobExists(givenfile);
            if (givenexistsinsplit) {
                boolean conflict = givenexistsinsplit(
                        givenfile, splitpoint, currhead, givenhead);
                if (conflict) {
                    System.out.println("Encountered a merge conflict.");
                    Utils.writeContents(new File(Utils.STAGED + givenfile),
                            Utils.readContents(new File(givenfile)));
                }
            } else {
                mergehelper2(givenfile, givenblob);
            }
        }
        currheadconflict(splitpoint, currhead, givenhead);
        mergehelper1(splitpoint, currhead, givenhead);
        String cm = "Merged " + givenbranch + " into " + currbranch + ".";
        List<String> files = Arrays.asList(new File(Utils.STAGED).list());
        Commit c = new Commit(cm, files);
        c.setParent2(givenhead.getSHA());
        commitTree.setHead(c.getSHA());
        c.initiate(c.getFilenames());
        commitTree.addCommit(c.getSHA());
        commitTree.serialize();
        clearStaged();
        return true;
    }

    /** Merge helper 3. Detects merge conflicts in currhead.
     * @param splitpoint commit obj at split point
     * @param currhead commit obj at head of current branch
     * @param givenhead commit obj at head of given branch
     * @return true if conflict detected. */
    boolean currheadconflict(
            Commit splitpoint, Commit currhead, Commit givenhead) {
        List<String> currheadfiles = currhead.getFilenames();
        for (String currheadfile: currheadfiles) {
            if (!givenhead.blobExists(currheadfile)) {
                if (splitpoint.blobExists(currheadfile)) {
                    Blob currblob = currhead.getBlob(currheadfile);
                    Blob splitblob = splitpoint.getBlob(currheadfile);
                    boolean currmodifiedsincesplit =
                            !Arrays.equals(currblob.getContents(),
                                    splitblob.getContents());
                    if (!currmodifiedsincesplit
                            && new File(currheadfile).exists()) {
                        Utils.restrictedDelete(new File(currheadfile));
                        new File(Utils.STAGED + currheadfile).delete();
                        return false;
                    }
                    System.out.println("Encountered a merge conflict.");
                    Utils.writeContents(new File(Utils.STAGED + currheadfile),
                            Utils.readContents(new File(currheadfile)));
                    writeConflictFile(currblob, currheadfile);
                    Utils.writeContents(new File(Utils.STAGED + currheadfile),
                            Utils.readContents(new File(currheadfile)));
                    return true;
                }
            }
        }
        return false;
    }

    /** Clears staging area. */
    void clearStaged() {
        for (String filename: new File(Utils.STAGED).list()) {
            File todelete = new File(Utils.STAGED + filename);
            todelete.delete();
        }
    }

    /** Helper method for merge in else case.
     * @param givenfile given file
     * @param givenblob blob of given file */
    void mergehelper2(String givenfile, Blob givenblob) {
        Utils.writeContents(
                new File(givenfile), givenblob.getContents());
        File tostage = new File(Utils.STAGED + givenfile);
        Utils.writeContents(tostage, givenblob.getContents());
    }

    /** Helper method for if a file at the head of
     * the given branch is present at the split point.
     * Returns true if a merge conflict was detected.
     * @param givenfile File that is at the head of the given branch
     * @param splitpoint Commit object at splitpoint
     * @param currhead Commit object at current branch head
     * @param givenhead Commit object at given branch head */
    boolean givenexistsinsplit(String givenfile, Commit splitpoint,
                                Commit currhead, Commit givenhead) {
        boolean conflictdetected = false;
        boolean givenexistsincurr = currhead.blobExists(givenfile);
        Blob givenblob = givenhead.getBlob(givenfile);
        Blob splitblob = splitpoint.getBlob(givenfile);
        boolean givenmodifiedsincesplit =
                !Arrays.equals(givenblob.getContents(),
                        splitblob.getContents());
        if (givenexistsincurr) {
            Blob currblob = currhead.getBlob(givenfile);
            boolean currmodifiedsincesplit =
                    !Arrays.equals(currblob.getContents(),
                            splitblob.getContents());
            if (givenmodifiedsincesplit && !currmodifiedsincesplit) {
                Utils.writeContents(
                        new File(givenfile), givenblob.getContents());
                File tostage = new File(Utils.STAGED + givenfile);
                Utils.writeContents(tostage, givenblob.getContents());
            } else if (givenmodifiedsincesplit && currmodifiedsincesplit) {
                boolean givencurrsamecontents =
                        Arrays.equals(givenblob.getContents(),
                                currblob.getContents());
                if (!givencurrsamecontents) {
                    conflictdetected = true;
                    try {
                        FileOutputStream output =
                                new FileOutputStream(givenfile);
                        byte[] sep =
                                System.getProperty("line.separator").getBytes();
                        output.write("<<<<<<< HEAD".getBytes());
                        output.write(sep);
                        output.write(currblob.getContents());
                        output.write("=======".getBytes());
                        output.write(sep);
                        output.write(givenblob.getContents());
                        output.write(">>>>>>>".getBytes());
                        output.write(sep);
                        output.close();
                    } catch (IOException excp) {
                        System.out.println("Could not write conflicted file.");
                    }
                }
            }
        }
        return conflictdetected;
    }

    /** Write an empty given conflicted file.
     * @param currblob blob obj at curr head
     * @param givenfile String name of given file */
    void writeConflictFile(Blob currblob, String givenfile) {
        try {
            FileOutputStream output =
                    new FileOutputStream(givenfile);
            byte[] sep = System.getProperty("line.separator").getBytes();
            output.write("<<<<<<< HEAD".getBytes());
            output.write(sep);
            output.write(currblob.getContents());
            output.write("=======".getBytes());
            output.write(sep);
            output.write(">>>>>>>".getBytes());
            output.write(sep);
            output.close();
        } catch (IOException excp) {
            System.out.println("Could not write conflicted file.");
        }
    }

    /** Remove and untrack files present at split point,
     * unmodified in current branch, and absent in the given branch.
     * @param splitpoint Commit object at split point
     * @param currhead Commit object at current branch head
     * @param givenhead Commit object at given branch head */
    void mergehelper1(Commit splitpoint, Commit currhead, Commit givenhead) {
        CommitTree commitTree = Utils.getCommitTree();
        List<String> splitpointfiles = splitpoint.getFilenames();
        for (String splitfile: splitpointfiles) {
            Blob splitblob = splitpoint.getBlob(splitfile);
            boolean splitexistsingiven = givenhead.blobExists(splitfile);
            boolean splitexistsincurr = currhead.blobExists(splitfile);
            if (!splitexistsingiven) {
                if (splitexistsincurr) {
                    Blob currblob = currhead.getBlob(splitfile);
                    boolean splitmodifiedincurr =
                            !Arrays.equals(currblob.getContents(),
                                    splitblob.getContents());
                    if (!splitmodifiedincurr) {
                        if (new File(splitfile).exists()) {
                            Utils.restrictedDelete(splitfile);
                        }
                        commitTree.addRmMarked(splitfile);
                        commitTree.serialize();
                        new File(Utils.STAGED + splitfile).delete();
                    }
                }
            }
        }
    }

    /** Find split point between given branch name
     * and current branch in CommitTree CT.
     * Returns the commit at the split point.
     * @param commitTree Commit Tree object
     * @param givenbranch name of given branch */
    Commit findSplitPoint(CommitTree commitTree, String givenbranch) {
        Commit currcommit = commitTree.getHeadCommit();
        Commit currhead = commitTree.getHeadCommit();
        Commit givencommit = commitTree.getHeadCommit(givenbranch);
        Commit givenhead = commitTree.getHeadCommit(givenbranch);
        HashSet<String> currcommitancestors = new HashSet<>();
        while (currcommit != null) {
            currcommitancestors.add(currcommit.getSHA());
            currcommit = currcommit.getParentCommit();
            if (currcommit.getParentSHA() == null) {
                break;
            }
        }
        while (givencommit != null) {
            if (currcommitancestors.contains(givencommit.getSHA())) {
                if (givencommit.equals(currhead)) {
                    System.out.println("Current branch fast-forwarded.");
                    return null;
                }
                if (givencommit.equals(givenhead)) {
                    System.out.println(
                            "Given branch is an ancestor "
                                    + "of the current branch.");
                    return null;
                }
                return givencommit;
            }
            givencommit = givencommit.getParentCommit();
            if (givencommit.getParentSHA() == null) {
                break;
            }
        }
        return null;
    }

    /** An input scanner from input source. */
    private String[] _input;

}
