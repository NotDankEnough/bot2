package kz.ilotterytea.bot.web.singletons;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import jakarta.inject.Singleton;
import kz.ilotterytea.bot.utils.StorageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Singleton
public class DocsSingleton {
    private final Logger LOG = LoggerFactory.getLogger(DocsSingleton.class);
    private HashMap<String, String> docs;

    public DocsSingleton() {
        this.docs = new HashMap<>();

        MutableDataSet options = new MutableDataSet();

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // im too lazy to fix my own code in StorageUtils :p
        List<String> rootFilePaths = StorageUtils.getFilepathsFromResource("/docs");
        List<String> filePaths = new ArrayList<>();

        for (String filePath : rootFilePaths) {
            try {
                List<String> filepaths = StorageUtils.getFilepathsFromResource(filePath);
                filePaths.addAll(filepaths);
            } catch (Exception ignored) {
                filePaths.add(filePath);
            }
        }

        for (String filePath : filePaths) {
            String contents = StorageUtils.readFileFromResources(filePath.substring(1));

            Node node = parser.parse(contents);
            String html = renderer.render(node);

            String[] parts = filePath.split("/");
            String name = parts[parts.length - 1];
            name = name.substring(0, name.length() - 3);

            this.docs.put(name, html);
        }
    }

    public String getDoc(String id) {
        return docs.getOrDefault(id, null);
    }
}
