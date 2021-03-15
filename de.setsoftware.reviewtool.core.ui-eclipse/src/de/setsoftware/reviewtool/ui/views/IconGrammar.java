package de.setsoftware.reviewtool.ui.views;

import org.eclipse.swt.graphics.GC;

/**
 * Allows the creation of various icons based on a very simple generational grammar.
 */
public class IconGrammar {

    private final BasicShape basicShape;
    private final LeafShape leaf1;
    private final LeafShape leaf2;

    /**
     * Enum for the basic structure of the icon.
     */
    private static enum BasicShape {
        SHAPE1(new int[][] {new int[] {0, 1}, new int[] {2, 0}}),
        SHAPE2(new int[][] {new int[] {1, 0}, new int[] {0, 2}}),
        SHAPE3(new int[][] {new int[] {0, 1, 0}, new int[] {0, 2, 0}, new int[] {0, 1, 0}}),
        SHAPE4(new int[][] {new int[] {0, 1, 0}, new int[] {1, 2, 1}, new int[] {0, 1, 0}}),
        SHAPE5(new int[][] {new int[] {0, 0, 0}, new int[] {1, 2, 1}, new int[] {0, 0, 0}}),
        SHAPE6(new int[][] {new int[] {1, 0, 2}, new int[] {1, 0, 2}, new int[] {1, 0, 2}}),
        SHAPE7(new int[][] {new int[] {1, 1, 1}, new int[] {0, 0, 0}, new int[] {2, 2, 2}}),
        SHAPE8(new int[][] {new int[] {0, 0, 1}, new int[] {2, 0, 0}, new int[] {0, 0, 1}}),
        SHAPE9(new int[][] {new int[] {1, 0, 0}, new int[] {0, 0, 2}, new int[] {1, 0, 0}}),
        SHAPE10(new int[][] {new int[] {1, 0, 1}, new int[] {0, 2, 0}, new int[] {1, 0, 1}}),
        SHAPE11(new int[][] {new int[] {1, 0, 0}, new int[] {1, 0, 0}, new int[] {0, 0, 2}}),
        SHAPE12(new int[][] {new int[] {0, 0, 1}, new int[] {0, 0, 1}, new int[] {2, 0, 0}});

        private int[][] fields;

        private BasicShape(int[][] fields) {
            this.fields = fields;
        }

        public void paint(GC gc, int height, int width, LeafShape leaf1, LeafShape leaf2) {
            for (int i = 0; i < this.fields.length; i++) {
                final int lower = height * i / this.fields.length;
                final int upper = height * (i + 1) / this.fields.length;
                for (int j = 0; j < this.fields[i].length; j++) {
                    final int left = width * j / this.fields[i].length;
                    final int right = width * (j + 1) / this.fields[i].length;
                    if (this.fields[i][j] == 1) {
                        leaf1.paint(gc, lower, upper, left, right);
                    } else if (this.fields[i][j] == 2) {
                        leaf2.paint(gc, lower, upper, left, right);
                    }
                }
            }
        }
    }

    /**
     * Enum for the shapes that can be filled into the basic structure.
     */
    private static enum LeafShape {
        LINE1 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.drawLine(left, upper, right, lower);
            }
        },
        LINE2 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.drawLine(left, lower, right, upper);
            }
        },
        LINE3 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.drawLine((left + right) / 2, upper, (left + right) / 2, lower);
            }
        },
        LINE4 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.drawLine(left, (lower + upper) / 2, right, (lower + upper) / 2);
            }
        },
        TWOLINE1 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.drawLine(left, upper, right, lower);
                gc.drawLine(left, lower, right, upper);
            }
        },
        TWOLINE2 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.drawLine((left + right) / 2, upper, (left + right) / 2, lower);
                gc.drawLine(left, (lower + upper) / 2, right, (lower + upper) / 2);
            }
        },
        TWOLINE3 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.drawLine(left, upper, left, lower);
                gc.drawLine(right, upper, right, lower);
            }
        },
        TWOLINE4 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.drawLine(left, lower, right, lower);
                gc.drawLine(left, upper, right, upper);
            }
        },
        BOX1 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.drawRectangle(left, lower, right - left, upper - lower);
            }
        },
        BOX2 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.fillRectangle(left, lower, right - left, upper - lower);
            }
        },
        BOX3 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.drawPolygon(new int[] {
                        (left + right) / 2, upper,
                        right, (lower + upper) / 2,
                        (left + right) / 2, lower,
                        left, (lower + upper) / 2
                });
            }
        },
        BOX4 {
            @Override
            public void paint(GC gc, int lower, int upper, int left, int right) {
                gc.fillPolygon(new int[] {
                        (left + right) / 2, upper,
                        right, (lower + upper) / 2,
                        (left + right) / 2, lower,
                        left, (lower + upper) / 2
                });
            }
        };

        public abstract void paint(GC gc, int lower, int upper, int left, int right);

    }

    private IconGrammar(BasicShape basic, LeafShape leaf1, LeafShape  leaf2) {
        this.basicShape = basic;
        this.leaf1 = leaf1;
        this.leaf2 = leaf2;
    }

    /**
     * Creates an icon for the given parameters.
     */
    public static IconGrammar create(int basic, int leaf1, int leaf2) {
        return new IconGrammar(
                val(BasicShape.values(), basic),
                val(LeafShape.values(), leaf1),
                val(LeafShape.values(), leaf2));
    }

    private static<T> T val(T[] arr, int i) {
        return arr[i % arr.length];
    }

    /**
     * Paints the icon in the given size into the given graphics context.
     */
    public void paint(GC gc, int height, int width) {
        this.basicShape.paint(gc, height, width, this.leaf1, this.leaf2);
    }

    @Override
    public String toString() {
        return this.basicShape + "," + this.leaf1 + "," + this.leaf2;
    }

    @Override
    public int hashCode() {
        return this.basicShape.hashCode() + 23 * this.leaf1.hashCode() + 2341 * this.leaf2.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IconGrammar)) {
            return false;
        }
        final IconGrammar g = (IconGrammar) o;
        return this.basicShape == g.basicShape
            && this.leaf1 == g.leaf1
            && this.leaf2 == g.leaf2;
    }
}
