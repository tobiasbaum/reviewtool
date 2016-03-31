package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * A slice of a unit of work that is in some way cohesive, for example all changes that belong to a certain
 * sub task.
 */
public class Slice {

    private List<Fragment> fragments;

}
