package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Lucy Yang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                Repository.initCommand();
                break;
            case "add":
                String addArg = args[1];
                Repository.addCommand(addArg);
                break;
            case "commit":
                String message = args[1];
                Repository.commitCommand(message, null, null);
                break;
            case "checkout":
                if (args.length == 3 && args[1].equals("--")) {
                    String fileName = args[2];
                    Repository.checkoutCommand(fileName);
                } else if (args.length == 4 && args[2].equals("--")) {
                    String commitID = args[1];
                    String fileName = args[3];
                    Repository.checkoutCommand(commitID, fileName);
                } else if (args.length == 2) {
                    String branchName = args[1];
                    Repository.checkoutBranchCommand(branchName);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "log":
                Repository.logCommand();
                break;
            case "rm":
                String fileName = args[1];
                Repository.rmCommand(fileName);
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "find":
                String commitMessage = args[1];
                Repository.findCommand(commitMessage);
                break;
            case "status":
                Repository.statusCommand();
                break;
            case "branch":
                String bn = args[1];
                Repository.branchCommand(bn);
                break;
            case "rm-branch":
                String branch = args[1];
                Repository.rmBranch(branch);
                break;
            case "reset":
                String uiD = args[1];
                Repository.resetCommand(uiD);
                break;
            case "merge":
                String branchie = args[1];
                Repository.mergeCommand(branchie);
                break;
            default:
                if (firstArg.isBlank()) {
                    System.out.println("Please enter a command.");
                } else {
                    System.out.println("No command with that name exists.");
                }
        }
    }
}
