package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/** A Commit object that points to blobs.
 * @author Jeff Xiang
 */
public class Commit implements Serializable {

    /** A new init commit. */
    Commit() {
        _parent = null;
        _parent2 = null;
        _message = "initial commit";
        _blobs = new HashMap<>();
        _calendar.setTimeZone(TimeZone.getDefault());
        _calendar.setTime(new Date(0));
        _filenames = new ArrayList<>();
        _sha = Utils.sha1(_message, _calendar.toString());
    }

    /** A new commit, not initiated.
     * @param message String of commit message
     * @param filenames String arraylist of file names to commit */
    Commit(String message, List<String> filenames) {
        CommitTree commitTree = Utils.getCommitTree();
        ArrayList<String> rmmarked = commitTree.getRmMarked();
        _parent = commitTree.getHeadSHA();
        _parent2 = null;
        _message = message;
        _calendar.setTimeZone(TimeZone.getDefault());
        List<String> toinclude =
                commitTree.getHeadCommit().getFilenames();
        List<String> parentfiles =
                commitTree.getHeadCommit().getFilenames();
        for (String parentfile: parentfiles) {
            if (rmmarked.contains(parentfile)) {
                toinclude.remove(parentfile);
            }
        }
        for (String staged: filenames) {
            if (!toinclude.contains(staged)) {
                toinclude.add(staged);
            }
        }
        _filenames = toinclude;
        HashMap<String, String> toincludeblobs =
                commitTree.getHeadCommit().getBlobs();
        for (String rmmarkedfile: rmmarked) {
            toincludeblobs.remove(rmmarkedfile);
        }
        _blobs = toincludeblobs;
        _sha = Utils.sha1(_filenames.toString(),
                _parent, _message, this.getDate());
    }

    /** Returns my SHA-1 value. */
    String getSHA() {
        return _sha;
    }

    /** Get commit message.
     * @return commit message */
    String getMessage() {
        return _message;
    }

    /** Returns my filenames. */
    List<String> getFilenames() {
        return _filenames;
    }

    /** Deserializes and returns the parent of this commit. */
    Commit getParentCommit() {
        return Utils.readObject(
                new File(Utils.SERIALIZED + _parent),
                Commit.class);
    }

    /** Returns _blobs. */
    HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /** Returns the parent SHA-1 value. */
    String getParentSHA() {
        return _parent;
    }

    /** Deserializes and returns the parent2 of this commit. */
    Commit getParent2Commit() {
        return Utils.readObject(
                new File(Utils.SERIALIZED + _parent2),
                Commit.class);
    }

    /** Returns the parent2 SHA-1 value. */
    String getParent2SHA() {
        return _parent2;
    }

    /** Places filenames in _blobs, mapping each filename to its blob SHA.
     * Simulatenously serializes each blob and this current commit.
     * @param filenames String list of file names */
    void initiate(List<String> filenames) {
        for (String filename: filenames) {
            File f = new File(Utils.STAGED + filename);
            if (f.exists()) {
                byte[] fcontents = Utils.readContents(f);
                Blob newblob = new Blob(fcontents, filename);
                String newblobSHA = newblob.getSHA();
                _blobs.put(filename, newblobSHA);
                newblob.serialize();
            }
        }

        this.serialize();
    }

    /** Returns the list of blobs tracked by this commit. */
    Object[] getTracked() {
        return _blobs.keySet().toArray();
    }

    /** Returns the string representation of my date. */
    String getDate() {
        String formatpattern = "EEE MMM dd HH:mm:ss yyyy Z";
        DateFormat format = new SimpleDateFormat(formatpattern);
        return format.format(_calendar.getTime());
    }

    /** Returns true iff a file in the current
     * working directory with FILENAME is
     * has the same contents as a blob contained
     * by this commit with the same FILENAME.
     * Assumes blob exists and filename exists
     * in current working directory.
     * @param workingfilename name of file in working directory
     */
    boolean sameContents(String workingfilename) {
        byte[] currdircontents = Utils.readContents(
                new File(workingfilename));
        Blob trackedblob = this.getBlob(workingfilename);
        byte[] trackedblobcontents = trackedblob.getContents();
        return Arrays.equals(currdircontents, trackedblobcontents);
    }

    /** Get my serialized file in .gitlet/serialized.
     * Assumes I am already serialized.
     * @return File object of serialized file of this commit */
    File getSerializedFile() {
        return new File(Utils.SERIALIZED + this._sha);
    }

    /** Set my parent 2.
     * @param sha String of parent 2 SHA-1 value */
    void setParent2(String sha) {
        _parent2 = sha;
    }

    /** Deserializes and returns the blob object pointed to by name.
     * @param name name of blob */
    Blob getBlob(String name) {
        String blobSHA = _blobs.get(name);
        File file = new File(Utils.SERIALIZED + blobSHA);
        return Utils.readObject(file, Blob.class);
    }

    /** Returns a boolean of whether a blob with name exists in this commit.
     * @param name name of blob */
    boolean blobExists(String name) {
        return _blobs.containsKey(name);
    }

    /** Serializes this commit object. */
    void serialize() {
        File f = new File(Utils.SERIALIZED + _sha);
        Utils.writeObject(f, this);
    }

    /** Checks if two commits are the same.
     * @param c2 Commit object
     * @return boolean of whether two commits are the same */
    boolean equals(Commit c2) {
        return this._sha.equals(c2.getSHA());
    }

    /** SHA value of this commit object. */
    private final String _sha;

    /** SHA of the parent of this commit. */
    private final String _parent;

    /** Calendar of this commit. */
    private final Calendar _calendar = Calendar.getInstance();

    /** Message of this commit. */
    private final String _message;

    /** HashMap of the name of blobs pointed to by
     * this commit. Values are the SHA of the blob. */
    private final HashMap<String, String> _blobs;

    /** Filenames (names of blobs) tracked by this commit. */
    private final List<String> _filenames;

    /** Parent two SHA-1 value. */
    private String _parent2;
}
