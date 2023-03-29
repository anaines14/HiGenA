package org.higena.ast.actions;

import java.util.Comparator;
import java.util.List;

public class EditActionsComparator implements Comparator<EditAction> {
  @Override
  public int compare(EditAction o1, EditAction o2) {
    String type1 = o1.getType(), type2 = o2.getType();
    List<String> category1 = List.of("TreeAddition", "TreeInsert", "Addition", "Insert");
    List<String> category2 = List.of("TreeDelete", "Delete");
    List<String> category3 = List.of("Update");
    List<String> category4 = List.of("Move");

    if (category1.contains(type1)) {
      if (category1.contains(type2)) {
        return 0;
      } else  {
        return -1;
      }
    }
    if (category2.contains(type1)) {
      if (category2.contains(type2)) {
        return 0;
      } if (category1.contains(type2)) {
        return 1;
      }
      else  {
        return -1;
      }
    }
    if (category3.contains(type1)) {
      if (category3.contains(type2)) {
        return 0;
      } if (category1.contains(type2) || category2.contains(type2)) {
        return 1;
      }
      else  {
        return -1;
      }
    }
    else {
      if (category4.contains(type2)) {
        return 0;
      } else {
        return 1;
      }
    }
  }
}
