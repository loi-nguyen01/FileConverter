import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FileConverter {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java FileConverter <input_file> <-c/-j/-x>");
            return;
        }

        String inputFile = args[0];
        String outputFormat = args[1];

        try {
            // Read the tab-delimited file
            List<String[]> data = readTabDelimitedFile(inputFile);

            // Check if the file contains any data
            if (data.isEmpty()) {
                System.out.println("Error: The file is empty or no data could be read.");
                return;
            }

            // Process based on the output format
            switch (outputFormat) {
                case "-c":
                    saveAsCSV(data, inputFile.replace(".txt", ".csv"));
                    break;
                case "-j":
                    saveAsJSON(data, inputFile.replace(".txt", ".json"));
                    break;
                case "-x":
                    saveAsXML(data, inputFile.replace(".txt", ".xml"));
                    break;
                default:
                    System.out.println("Unknown format. Use -c for CSV, -j for JSON, -x for XML");
            }
        } catch (IOException | ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    // Method to read a tab-delimited file
    public static List<String[]> readTabDelimitedFile(String fileName) throws IOException {
        List<String[]> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                data.add(line.split("\t"));
            }
        }
        return data;
    }

    // Save data as CSV
    public static void saveAsCSV(List<String[]> data, String outputFile) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            for (String[] row : data) {
                bw.write(String.join(",", row));
                bw.newLine();
            }
        }
        System.out.println("Saved as CSV: " + outputFile);
    }

    // Save data as JSON (without Gson)
    public static void saveAsJSON(List<String[]> data, String outputFile) throws IOException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[\n");

        String[] headers = data.get(0);

        for (int i = 1; i < data.size(); i++) {
            jsonBuilder.append("  {\n");
            for (int j = 0; j < headers.length; j++) {
                jsonBuilder.append("    \"").append(headers[j]).append("\": \"").append(data.get(i)[j]).append("\"");
                if (j < headers.length - 1) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append("\n");
            }
            jsonBuilder.append("  }");
            if (i < data.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }

        jsonBuilder.append("]\n");

        // Write the JSON string to the output file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            bw.write(jsonBuilder.toString());
        }

        System.out.println("Saved as JSON: " + outputFile);
    }

    // Save data as XML
    public static void saveAsXML(List<String[]> data, String outputFile)
            throws ParserConfigurationException, TransformerException, IOException {

        // Create XML document
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        Element root = document.createElement("root");
        document.appendChild(root);

        // Headers
        String[] headers = data.get(0);
        System.out.println("Original headers: " + Arrays.toString(headers));

        // Sanitize header names for valid XML tags
        String[] sanitizedHeaders = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            sanitizedHeaders[i] = sanitizeForXML(headers[i]);  // Replace invalid characters for XML tag names
        }
        System.out.println("Sanitized headers: " + Arrays.toString(sanitizedHeaders));

        // Rows
        for (int i = 1; i < data.size(); i++) {
            Element item = document.createElement("item");
            root.appendChild(item);
            System.out.println("Processing row: " + Arrays.toString(data.get(i)));

            for (int j = 0; j < sanitizedHeaders.length; j++) {
                Element element = document.createElement(sanitizedHeaders[j]);
                String content = escapeXMLCharacters(data.get(i)[j]);
                System.out.println("Adding element: " + sanitizedHeaders[j] + " with content: " + content);
                element.appendChild(document.createTextNode(content));  // Escape special characters in content
                item.appendChild(element);
            }
        }

        // Create the XML file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new File(outputFile));

        transformer.transform(domSource, streamResult);
        System.out.println("Saved as XML: " + outputFile);
    }

    // Sanitize header names to ensure valid XML tags
    private static String sanitizeForXML(String header) {
        // Replace any non-alphanumeric character with an underscore
        String sanitized = header.replaceAll("[^a-zA-Z0-9]", "_");

        // If the sanitized header starts with a digit, prepend an underscore
        if (Character.isDigit(sanitized.charAt(0))) {
            sanitized = "_" + sanitized;
        }

        return sanitized;
    }

    // Escape special characters in XML content
    private static String escapeXMLCharacters(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
