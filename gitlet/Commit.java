package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author LUCY YANG
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The time and date of this Commit. */
    private Date timestamp;

    /** The message of this Commit. */
    private String message;

    private TreeMap<String, String> commitLibrary;

    private TreeMap<String, String> branches;

    //head pointer, master pointer

    private String head;

    //public String parent;

    private ArrayList<String> parent;

    //private Commit branch;

    private String UID;

    private boolean merged;

    public static final File HEAD = join(Repository.GITLET_DIR, "headCommit");

    public static final File ACTIVE_BRANCH = join(Repository.GITLET_DIR, "activeBranch");

    public static final File BRANCHES = join(Repository.GITLET_DIR, "branches");

    public Commit() {
        this.message = "initial commit";
        this.timestamp = new Date(0);
        this.merged = false;
        parent = null;
        //this.parent = null;
        head = sha1(serialize(this));
        UID = head;
        writeContents(HEAD, head);
        commitLibrary = new TreeMap<>();
        branches = new TreeMap<>();
        writeObject(BRANCHES, branches);
    }

    public Commit(String message, Date timestamp) {
        this.message = message;
        this.timestamp = timestamp;
        this.merged = false;
        parent = new ArrayList<String>();
        parent.add(readContentsAsString(HEAD));
        head = sha1(serialize(this));
        UID = head;
        writeContents(HEAD, head);
        commitLibrary = new TreeMap<>();
        //writeObject(LIBRARY, commitLibrary);
    }

    public Commit(String message, Date timestamp, String uiD1, String uiD2) {
        this.message = message;
        this.timestamp = timestamp;
        this.merged = true;
        parent = new ArrayList<String>();
        parent.add(uiD1);
        parent.add(uiD2);
        head = sha1(serialize(this));
        UID = head;
        writeContents(HEAD, head);
        commitLibrary = new TreeMap<>();
    }

    public String getParent() {
        if (this.parent != null) {
            return this.parent.get(0);
        } else {
            return null;
        }
    }

    public ArrayList<String> getParentList() {
        return this.parent;
    }

    public String getMessage() {
        return this.message;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public boolean aResultOfMerge() {
        return this.merged;
    }

    public TreeMap getCommitLibrary() {
        //return readObject(LIBRARY, HashMap.class);
        return this.commitLibrary;
    }

    public static String getHead() {
        return readContentsAsString(HEAD);
    }

    public String getUID() {
        return this.UID;
    }

    public String getDate() {
        String pattern = "EEE MMM d HH:mm:ss yyyy Z";
        SimpleDateFormat simpleDate = new SimpleDateFormat(pattern);
        return simpleDate.format(this.timestamp);
    }
}
