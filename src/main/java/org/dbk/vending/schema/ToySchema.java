package org.dbk.vending.schema;


import lombok.*;
import org.apache.commons.text.StringEscapeUtils;

import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.*;
import java.util.List;


@XmlRootElement
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ToySchema {
    static JAXBContext jaxbContext = null;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(ToySchema.class, Animation.class, Table.class, Row.class, Item.class, Image.class);
        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }

    private Table table;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Animation {
        private String path;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Row {
        private List<Item> item;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static class Item {
        private String id;
        private int position;
        private int price;
        private Image image;
        private Animation animation;
        private Description description;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Image {
        private String path;
    }

    public static ToySchema parse(String xml) {
        try {
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (ToySchema) jaxbUnmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String toXml() {

        try {
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            jaxbMarshaller.marshal(this, writer);
            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Description {
        String text;


        public Description(String text) {
            this.text = StringEscapeUtils.escapeXml11(text);;
        }

        public void setText(String text) {

            this.text = StringEscapeUtils.escapeXml11(text);;
        }

        public String getText() {
            return StringEscapeUtils.unescapeXml(text);
        }
    }
}
