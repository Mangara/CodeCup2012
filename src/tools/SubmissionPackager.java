package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author Sander Verdonschot
 */
public class SubmissionPackager {

    private JFileChooser sourceFileChooser;
    private JFileChooser outputFileChooser;
    private String outputFileName;
    private static final boolean MINIMIZE = true;

    public static void main(String[] args) throws IOException {
        new SubmissionPackager();
    }

    public SubmissionPackager() throws IOException {
        // Select which java files should be included
        List<File> sourceFiles = selectSourceFiles();

        if (sourceFiles != null && !sourceFiles.isEmpty()) {
            // Select the main class among the included classes with a main method, if there are multiple
            File mainClass = selectMainClass(sourceFiles);

            // Choose a name and location for the output file
            File outputFile = selectOutputFile();

            if (mainClass != null && outputFile != null) {
                // Gather all necessary imports
                Set<String> imports = gatherNecessaryImports(sourceFiles);

                // Write everything
                BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));

                // Imports first
                for (String line : imports) {
                    out.write(line);
                    out.newLine();
                }

                if (!MINIMIZE) {
                    out.newLine();
                }

                // Then the main class
                appendClass(mainClass, out, true);

                // Finally the others
                for (File file : sourceFiles) {
                    if (file != mainClass) {
                        appendClass(file, out, false);
                    }
                }

                out.close();
            }
        }
    }

    private List<File> selectSourceFiles() throws IOException {
        boolean more = true;
        List<File> files = new ArrayList<File>();

        // Initialize sourceFileChooser
        sourceFileChooser = new JFileChooser(System.getProperty("user.dir"));
        sourceFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Java Source Files", "java"));
        sourceFileChooser.setMultiSelectionEnabled(true);

        while (more) {
            int opened = sourceFileChooser.showOpenDialog(null);

            if (opened == JFileChooser.APPROVE_OPTION) {
                files.addAll(Arrays.asList(sourceFileChooser.getSelectedFiles()));
            }

            int option = JOptionPane.showConfirmDialog(null, "Do you want to include more files?", "Submission Packager", JOptionPane.YES_NO_OPTION);

            if (option != JOptionPane.YES_OPTION) {
                more = false;
            }
        }

        return files;
    }

    private String[] getMainClassCandidates(List<File> sourceFiles) throws IOException {
        ArrayList<String> candidates = new ArrayList<String>();

        for (File file : sourceFiles) {
            BufferedReader in = new BufferedReader(new FileReader(file));

            String line = in.readLine();

            while (line != null) {
                if (line.contains("public static void main(")) {
                    candidates.add(file.getName());
                    break;
                }

                line = in.readLine();
            }

            in.close();
        }

        return candidates.toArray(new String[candidates.size()]);
    }

    private File selectMainClass(List<File> sourceFiles) throws IOException {
        String[] mainCandidates = getMainClassCandidates(sourceFiles);
        String mainClassName;

        if (mainCandidates.length == 0) {
            throw new IllegalArgumentException("None of the classes have a main method.");
        } else if (mainCandidates.length == 1) {
            mainClassName = mainCandidates[0];
        } else {
            mainClassName = (String) JOptionPane.showInputDialog(null, "Select the main class:", "Submission Packager", JOptionPane.QUESTION_MESSAGE, null, mainCandidates, mainCandidates[0]);
        }

        // Map name -> file
        for (File file : sourceFiles) {
            if (mainClassName.equals(file.getName())) {
                return file;
            }
        }

        return null;
    }

    private File selectOutputFile() {
        // Initialize outputFileChooser
        outputFileChooser = new JFileChooser(System.getProperty("user.dir"));
        outputFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Java Source Files", "java"));
        outputFileChooser.setMultiSelectionEnabled(false);

        int saved = outputFileChooser.showSaveDialog(null);

        if (saved == JFileChooser.APPROVE_OPTION) {
            File outputFile = outputFileChooser.getSelectedFile();
            outputFileName = outputFile.getName();

            if (!outputFileName.endsWith(".java")) {
                outputFileName += ".java";
                outputFile = new File(outputFile.getParentFile(), outputFileName);
            }

            return outputFile;
        } else {
            return null;
        }
    }

    private Set<String> gatherNecessaryImports(List<File> sourceFiles) throws IOException {
        // Add all imports that aren't of included classes
        HashSet<String> imports = new HashSet<String>();

        for (File file : sourceFiles) {
            BufferedReader in = new BufferedReader(new FileReader(file));

            String line = in.readLine();

            while (line != null) {
                if (line.contains("import java") || line.contains("import sun")) {
                    imports.add(line);
                }

                if (line.contains("{")) {
                    // Class starts; no imports after this
                    break;
                }

                line = in.readLine();
            }

            in.close();
        }

        return imports;
    }

    private void appendClass(File sourceFile, BufferedWriter out, boolean isMainClass) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(sourceFile));

        String line = in.readLine();

        // Skip to the start of the class
        while (line != null && !line.contains("{")) {
            line = in.readLine();
        }

        // Remove 'public' if this is not the main class
        if (isMainClass) {
            // Replace the name of the main class with the name of the output file
            String className = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.'));
            out.write(line.replace(className, outputFileName.substring(0, outputFileName.lastIndexOf('.'))));
            out.newLine();
        } else {
            out.write(line.trim().substring("public ".length()));
            out.newLine();
        }

        // Copy the remainder of the file
        line = in.readLine();

        while (line != null) {
            if (MINIMIZE) {
                // Remove indentation and comments
                line = pack(line);

                while (line.contains("/*") && line.contains("*/")) {
                    int start = line.indexOf("/*");
                    int end = line.indexOf("*/", start + 2);

                    if (end != -1) {
                        line = line.substring(0, start).trim() + " " + line.substring(end + 2).trim();
                    }
                }

                if (line.contains("/*")) {
                    // Start of a multi-line block comment
                    line = line.substring(0, line.indexOf("/*"));
                    String temp = in.readLine();

                    while (!temp.contains("*/")) {
                        temp = in.readLine();
                    }

                    temp = temp.substring(temp.indexOf("*/") + 2);

                    if (temp.contains("/*")) {
                        System.err.println("WARNING: new block comment starting on the same line where the previous finished. This case isn't handled correctly by the code minimizer. It is recommended that you start the new block comment on the next line instead.");
                    }

                    line += pack(temp);
                }
            }

            // Skip empty lines when minimizing
            if (!MINIMIZE || !line.isEmpty()) {
                out.write(line);
                out.newLine();
            }

            line = in.readLine();
        }

        if (!MINIMIZE) {
            out.newLine();
        }

        in.close();
    }

    private String pack(String line) {
        return line.replaceAll("//.*$", "").replaceAll("([\\(\\)\\{\\}\\[\\],+\\-/%*=?:;<>|\\&\\^!]) ", "$1").replaceAll(" ([\\(\\)\\{\\}\\[\\],+\\-/%*=?:;<>|\\&\\^!])", "$1").trim();
    }
}
