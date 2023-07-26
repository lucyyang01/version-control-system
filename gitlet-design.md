# Gitlet Design Document

**Name**: Lucy Yang

## Classes and Data Structures

###Main
Where each command is called. 
####Fields
No fields. 

### Repository
This is where each command is implemented, in order of the spec.
#### Fields

1. CWD: the current working directory of the user. 
2. GITLET_DIR: the .gitlet directory
3. STAGING_ADD: file that holds the staging area (add) treemap.
4. STAGING_RM: the file that holds the staging area (remove) treemap. 
5. REPOSITORY: the file that holds the repository treemap. 
6. ADD_AREA: the treemap for the staging area for files staged to be added. 
7. REMOVE_AREA: the treemap for the staging area for files staged to be removed. 
8. REPO: the treemap for the repository. 
9. BLOBS: treemap that contains the blobHashes and contents of blobs.


### Commit
This handles everything to do relating to commits, such as specific commit instances and keeping track of branches, head.
#### Fields

1. timestamp: Date variable of the date of commit.
2. message: String message of this commit. 
3. commitLibrary: A Treemap<String, String> that will map fileNames to blobs in each commit.
4. head: keeps track of the head commit with its sha1 (type String).
5. parent: keeps track of the parent Commit of the current commit.
7. HEAD: file that houses the Head commit. 
8. UID: contains this commit instance's UID
9. ACTIVE_BRANCH: file to save string name of active branch
10. BRANCHES: file to save branches treemap
11. branches: treemap containing branch names and the head commit in each branch


## Algorithms

## Persistence
Persistence is maintained in Repository and Commit classes. Repository serializes and writes to disk every single treemap in my code. Commit serializes and writes to disk the Head commit, which it saves to the HEAD file. 
hi