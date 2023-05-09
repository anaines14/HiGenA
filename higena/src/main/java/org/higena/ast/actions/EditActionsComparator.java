package org.higena.ast.actions;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Class that compares two edit actions based on their type.
 *
 * @see EditAction
 */
public class EditActionsComparator implements Comparator<EditAction> {

  /**
   * Compares two edit actions based on their type. The order is the following:
   * 1. TreeAddition, TreeInsert, Addition, Insert
   * 2. TreeDelete, Delete
   * 3. Update
   * 4. Move
   *
   * @param o1 the first EditAction to be compared.
   * @param o2 the second EditAction to be compared.
   * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
   */
  @Override
  public int compare(EditAction o1, EditAction o2) {
    String type1 = o1.getType(), type2 = o2.getType();
    List<String> category1 = Arrays.asList("TreeAddition", "TreeInsert", "Addition",
            "Insert");
    List<String> category2 = Arrays.asList("TreeDelete", "Delete");
    String category3 = "Update";

    // type1 = type2
    if (type1.equals(type2)) {
      return 0;
    }

    if (category1.contains(type1)) {
      if (category1.contains(type2)) {
        // type1 = Addition, type2 = Addition
        return 0;
      }
      // type1 = Addition, type2 = Delete, Update or Move
      return -1;
    }
    if (category2.contains(type1)) {
      if (category2.contains(type2)) {
        // type1 = Delete, type2 = Delete
        return 0;
      }
      if (category1.contains(type2)) {
        // type1 = Delete, type2 = Addition
        return 1;
      } else {
        // type1 = Delete, type2 = Update or Move
        return -1;
      }
    }
    if (category3.equals(type1)) {
      if (category1.contains(type2) || category2.contains(type2)) {
        // type1 = Update, type2 = Addition or Delete
        return 1;
      } else {
        // type1 = Update, type2 = Move
        return -1;
      }
    }
    // type1 = Move, type2 = Addition, Delete or Update
    return 1;
  }
}
