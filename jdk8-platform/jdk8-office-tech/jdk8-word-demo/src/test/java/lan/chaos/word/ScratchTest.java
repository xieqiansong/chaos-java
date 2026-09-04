package lan.chaos.word;

import lan.chaos.word.common.config.WordProperties;
import lan.chaos.word.common.util.OutFiles;
import lan.chaos.word.template.TemplateFillService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;

class ScratchTest {

    @Test
    void dump() throws Exception {
        OutFiles outFiles = new OutFiles(new WordProperties());
        new TemplateFillService(outFiles).run();
        for (String name : new String[]{"word-template.docx", "word-template-naive.docx", "word-template-filled.docx"}) {
            File doc = outFiles.of(name);
            System.out.println("==== " + name + " exists=" + doc.exists());
            try (XWPFDocument d = new XWPFDocument(new FileInputStream(doc))) {
                for (XWPFParagraph p : d.getParagraphs()) {
                    String t = p.getText();
                    System.out.println("  P text=[" + t + "] contains(${author})=" + (t != null && t.contains("${author}")) + " contains(张三)=" + (t != null && t.contains("张三")));
                }
            }
        }
    }
}
