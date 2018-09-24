package gitlet;

import java.io.File;
import java.io.Serializable;

/** An object that stores contents of files.
 * @author Jeff Xiang
 */
public class Blob implements Serializable {

    /** A new blob object.
     * @param contents byte array of contents
     * @param filename name of file */
    Blob(byte[] contents, String filename) {
        _contents = contents;
        _name = filename;
        _sha = Utils.sha1(_contents, filename);
    }

    /** Deserializes me and returns a byte array of my contents.
     * Assumes I am already serialized. */
    byte[] getContents() {
        return _contents;
    }

    /** Returns my serialized file in .gitlet/serialized.
     * Assumes I am already serialized. */
    File getSerializedFile() {
        return new File(Utils.SERIALIZED + this._sha);
    }

    /** Serialize this blob. */
    void serialize() {
        File f = new File(Utils.SERIALIZED + _sha);
        Utils.writeObject(f, this);
    }

    /** Name of this blob.
     * @return String */
    String getName() {
        return _name;
    }

    /** Returns the SHA-1 value of this blob. */
    String getSHA() {
        return _sha;
    }

    /** The SHA-1 value of this blob. */
    private final String _sha;

    /** The contents of this blob. */
    private final byte[] _contents;

    /** The name of this blob. */
    private final String _name;
}
