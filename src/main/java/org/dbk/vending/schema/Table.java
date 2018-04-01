package org.dbk.vending.schema;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Table {

    private int columns;
    private int rows;

    private List<ToySchema.Row> rowData;
}
