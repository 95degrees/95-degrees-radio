package me.voidinvoid.karaoke;

import me.voidinvoid.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class Lyrics {

    private List<Lyric> lyrics = new ArrayList<>();

    public Lyrics(String rawXml) throws ParserConfigurationException, SAXException, IOException {

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(rawXml)));
        doc.getDocumentElement().normalize();

        NodeList transRoot = doc.getElementsByTagName("transcript").item(0).getChildNodes();

        for (int i = 0; i <= transRoot.getLength(); i++) {
            Node node = transRoot.item(i);

            if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {

                Element lyric = (Element) node;
                lyrics.add(new Lyric(lyric.getFirstChild() == null ? "" : lyric.getFirstChild().getTextContent(), Double.valueOf(lyric.getAttribute("start")), Double.valueOf(lyric.getAttribute("dur"))));
            }
        }

        System.out.println("Found lyrics for song!");
    }

    public String getMessage(String songName, double elapsed) {
        Lyric active = getActiveLyric(elapsed);

        StringBuilder builder = new StringBuilder("[").append(Utils.escape(songName)).append(" Lyrics]\n\n");

        for (Lyric l : lyrics) {
            builder.append(l.equals(active) ? "➡" : "◼").append(Utils.escape(l.getText())).append("\n");
        }

        System.out.println(builder.toString());

        return builder.toString();
    }

    public double getNextLyricEntryTime(double elapsed) {
        for (Lyric l : lyrics) {
            if (l.getEntryTime() > elapsed) return l.getEntryTime();
        }

        return -1;
    }

    public List<Lyric> getLyrics() {
        return lyrics;
    }

    public Lyric getActiveLyric(double elapsed) {
        for (Lyric l : lyrics) {
            if ((l.getEntryTime() <= elapsed) && (l.getEntryTime() + l.getLength() >= elapsed)) return l;
        }

        return null;
    }
}
