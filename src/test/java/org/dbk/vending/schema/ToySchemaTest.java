package org.dbk.vending.schema;

import junit.framework.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ToySchemaTest {


    @Test
    public void testXml() {

        List<ToySchema.Row> rows = IntStream.range(0, 3)
                .mapToObj(row -> new ToySchema.Row(IntStream.range(0, 4)
                        .mapToObj(column -> {
                                    String id = row + " " + column;
                                    int position = row * 4 + column;
                                    int price = (column + 1) * 100;
                                    ToySchema.Image image = new ToySchema.Image(String.format("/toy%d.png", column));
                                    ToySchema.Description descpription = new ToySchema.Description();
                                    descpription.setText("<style>\n" +
                                            "body {\n" +
                                            "    background-color: #F4F4F4;\n" +
                                            "}\n" +
                                            "</style>\n" +
                                            "</head>\n" +
                                            "<body>\n" +
                                            "\n" +
                                            "<h1>Any toy</h1>\n" +
                                            "\n" +
                                            "<div>very very interesting toy</div>\n" +
                                            "\n" +
                                            "<p>Set a <span>background color</span> for only a part of a text.</p>\n" +
                                            "\n" +
                                            "</body>\n");
                                    ToySchema.Animation animation = ToySchema.Animation.builder().path(String.format("/anim%d.mp4", row)).build();
                                    return new ToySchema.Item(id, position, price, image, animation, descpription);
                                }
                        )
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
        ToySchema toySchema = ToySchema.builder()
                .table(Table.builder()
                        .columns(4)
                        .rows(3)
                        .rowData(rows)
                        .build())
                .build();

        String xml = toySchema.toXml();
        System.out.println(xml);
        ToySchema parse = ToySchema.parse(xml);
        Assert.assertEquals(toySchema, parse);
    }
}