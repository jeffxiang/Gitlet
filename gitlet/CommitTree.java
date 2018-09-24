package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/** A Commit tree object that points to commits.
 * @author Jeff Xiang
 */
public class CommitTree implements Serializable {

    /** A new CommitTree object.
     * @param head head commit of this commit tree */
    CommitTree(Commit head) {
        String headsha = head.getSHA();
        _branches = new ArrayList<>();
        _headBranch = "master";
        _branchMap = new HashMap<>();
        _branchMap.put(_headBranch, headsha);
        _branches.add(_headBranch);
        _rmMarked = new ArrayList<>();
        _commits = new ArrayList<>();
        _commits.add(headsha);
        _removed = new ArrayList<>();
    }

    /** Deserializes and returns the head commit
     * object of headbranch of this CommitTree. */
    Commit getHeadCommit() {
        String headsha = _branchMap.get(_headBranch);
        File file = new File(Utils.SERIALIZED + headsha);
        return Utils.readObject(file, Commit.class);
    }

    /** Deserializes and returns the
     * head commit object of a BRANCH of this CT.
     * @param branchname String of branch name*/
    Commit getHeadCommit(String branchname) {
        String headsha = _branchMap.get(branchname);
        File file = new File(Utils.SERIALIZED + headsha);
        return Utils.readObject(file, Commit.class);
    }

    /** Sets the head of this commit tree a new SHA value.
     * @param sha string of SHA of commit to set head to */
    void setHead(String sha) {
        String headsha = sha;
        _branchMap.replace(_headBranch, headsha);
    }

    /** Sets the head branch name of this commit tree.
     * @param name set the head branch name */
    void setHeadBranchName(String name) {
        _headBranch = name;
    }

    /** Adds a branch with BRANCHNAME to current head.
     * @param branchname name of added branch */
    void addBranch(String branchname) {
        _branchMap.put(branchname, this.getHeadSHA());
        _branches.add(branchname);
    }

    /** Removes branch with BRANCHNAME.
     * @param branchname name of removed branch */
    void rmBranch(String branchname) {
        _branchMap.remove(branchname);
        _branches.remove(branchname);
    }

    /** Get name of head branch of this CT.
     * @return String of head branch name */
    String getHeadName() {
        return _headBranch;
    }

    /** Add a commit SHA to _commits.
     * @param sha SHA-1 value of added commit */
    void addCommit(String sha) {
        _commits.add(sha);
    }

    /** Returns the list of commits contained in this CommitTree. */
    ArrayList<String> getCommits() {
        return _commits;
    }

    /** Returns the list of filenames that are marked to be removed. */
    ArrayList<String> getRmMarked() {
        return _rmMarked;
    }

    /** Adds a filename to _rmMarked.
     * @param filename String of file name to add to rmMarked */
    void addRmMarked(String filename) {
        _rmMarked.add(filename);
    }

    /** Removes a filename from _rmMarked.
     * @param filename String of file name ot rm from rmMarked */
    void removeRmMarked(String filename) {
        _rmMarked.remove(filename);
    }

    /** Return the SHA-1 value of the head of this commit. */
    String getHeadSHA() {
        return _branchMap.get(_headBranch);
    }

    /** Returns the name of the head branch. */
    String getHeadBranchName() {
        return _headBranch;
    }

    /** Adds a filename to _removed.
     * @param filename String of file name to add to _removed */
    void addRemoved(String filename) {
        _removed.add(filename);
    }

    /** Returns rmMarked. */
    ArrayList<String> getRemoved() {
        return _rmMarked;
    }

    /** Clears _rmMarked. */
    void clearrmMarked() {
        _rmMarked.clear();
    }

    /** Returns _branchMap. */
    HashMap<String, String> getBranchMap() {
        return _branchMap;
    }

    /** Returns _branches. */
    ArrayList<String> getBranches() {
        return _branches;
    }

    /** Serialize this commit tree. */
    void serialize() {
        File f = new File(Utils.SERIALIZED + "CT");
        Utils.writeObject(f, this);
    }

    /** Deserializes and returns the commit
     * with SHA (can be in shortened form).
     * If commit with this SHA doesn't exist,
     * print "No commit with that id exists."
     * and return null.
     * @param sha SHA-1 value of commit */
    Commit getCommit(String sha) {
        String[] serializedfiles = new File(Utils.SERIALIZED).list();
        if (serializedfiles != null) {
            for (String serializedfile: serializedfiles) {
                if (!serializedfile.equals("CT")
                        && serializedfile.contains(sha)) {
                    return Utils.readObject(
                            new File(Utils.SERIALIZED + serializedfile),
                            Commit.class);
                }
            }
        }
        System.out.println("No commit with that id exists.");
        return null;
    }

    /** A HashMap of the contents of this commit tree. Keys are name of branch,
     * values are SHA of the head commit of that branch.
     */
    private HashMap<String, String> _branchMap;

    /** An ArrayList of all branch names. */
    private ArrayList<String> _branches;

    /** An ArrayList of all commit SHA's in this commit tree. */
    private ArrayList<String> _commits;

    /** An ArrayList of filenames that are marked
     * to NOT be included in the next commit. */
    private ArrayList<String> _rmMarked;

    /** An ArrayList of filenames that have been removed. */
    private ArrayList<String> _removed;

    /** Name of head branch of this CT. */
    private String _headBranch;
}
