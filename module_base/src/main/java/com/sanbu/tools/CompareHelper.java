package com.sanbu.tools;

import java.util.List;

public class CompareHelper {
    public interface NotNullComparator<T> {
        boolean isEqual(T src, T dst);
    }

    public static <T> boolean isEqual(T src, T dst, NotNullComparator<T> comparator) {
        if (src == null && dst == null)
            return true;
        if (src == null && dst != null)
            return false;
        if (src != null && dst == null)
            return false;
        return comparator.isEqual(src, dst);
    }

    public static boolean isEqual(String src, String dst) {
        return isEqual(src, dst, false);
    }

    public static boolean isEqual(final String src, final String dst, final boolean ignoreCase) {
        return isEqual(src, dst, new NotNullComparator() {
            @Override
            public boolean isEqual(Object str1, Object str2) {
                if (ignoreCase)
                    return src.equalsIgnoreCase(dst);
                else
                    return src.equals(dst);
            }
        });
    }

    public static boolean isEqual(final Enum src, final Enum dst) {
        return isEqual(src, dst, new NotNullComparator() {
            @Override
            public boolean isEqual(Object enum1, Object enum2) {
                return src == dst;
            }
        });
    }

    public static boolean isEqual(final Number src, final Number dst) {
        return isEqual(src, dst, new NotNullComparator() {
            @Override
            public boolean isEqual(Object num1, Object num2) {
                return src.equals(dst);
            }
        });
    }

    public static boolean isEqual(final Boolean src, final Boolean dst) {
        return isEqual(src, dst, new NotNullComparator() {
            @Override
            public boolean isEqual(Object boolean1, Object boolean2) {
                return src == dst;
            }
        });
    }

    public static boolean isEqual(final List<String> src, final List<String> dst) {
        return isEqual(src, dst, new NotNullComparator() {
            @Override
            public boolean isEqual(Object i1, Object i2) {
                if (src.size() != dst.size())
                    return false;
                for (int i = 0; i < src.size(); ++i) {
                    if (!CompareHelper.isEqual(src.get(i), dst.get(i)))
                        return false;
                }
                return true;
            }
        });
    }

    public static <T> boolean isEqual4BaseList(final List<T> src, final List<T> dst) {
        return isEqual(src, dst, new NotNullComparator() {
            @Override
            public boolean isEqual(Object i1, Object i2) {
                if (src.size() != dst.size())
                    return false;

                boolean checked = false;
                for (int i = 0; i < src.size(); ++i) {
                    if (!checked) {
                        T obj = src.get(i);
                        if (!(obj instanceof Number) && !(obj instanceof Boolean) &&
                                !(obj instanceof Enum) && !(obj instanceof String))
                            throw new UnsupportedOperationException("type of list item MUST be String/Number/Boolean/Enum");

                        checked = true;
                    }

                    if (!CompareHelper.isEqual(src.get(i), dst.get(i), new NotNullComparator<T>() {
                        @Override
                        public boolean isEqual(T src, T dst) {
                            return src.equals(dst);
                        }
                    })) {
                        return false;
                    }
                }

                return true;
            }
        });
    }

    public static <T> boolean isEqual4List(final List<T> src, final List<T> dst, final NotNullComparator<T> comparator) {
        return isEqual(src, dst, new NotNullComparator() {
            @Override
            public boolean isEqual(Object i1, Object i2) {
                if (src.size() != dst.size())
                    return false;

                for (int i = 0; i < src.size() ; ++i) {
                    if (!comparator.isEqual(src.get(i), dst.get(i)))
                        return false;
                }

                return true;
            }
        });
    }

    public static <T> boolean equals(T src1, Object src2, NotNullComparator<T> comparator) {
        if (src2 == null)
            return false;
        if (src1 == src2)
            return true;
        if (!src1.getClass().equals(src2.getClass()))
            return false;
        return comparator.isEqual(src1, (T) src2);
    }

    //// Testing
    // enum E1 {
    //     aa,
    //     bb
    // };
    // public static void main(String[] args) {
    //     String str1 = "1";
    //     String str2 = "1";
    //     String str3 = "12";
    //     String str4 = null;
    //     String str5 = null;
    //     System.out.println("T: " + CompareHelper.isEqual("1", "1"));
    //     System.out.println("F: " + CompareHelper.isEqual("1", "12"));
    //     System.out.println("F: " + CompareHelper.isEqual(null, "1"));
    //     System.out.println("F: " + CompareHelper.isEqual("1", null));
    //     System.out.println("T: " + CompareHelper.isEqual(str1, str2));
    //     System.out.println("F: " + CompareHelper.isEqual(str1, str3));
    //     System.out.println("F: " + CompareHelper.isEqual(str3, str4));
    //     System.out.println("F: " + CompareHelper.isEqual(str4, str3));
    //     System.out.println("T: " + CompareHelper.isEqual(str4, str5));
    //     E1 e1 = E1.aa;
    //     E1 e2 = E1.aa;
    //     E1 e3 = E1.bb;
    //     E1 e4 = null;
    //     E1 e5 = null;
    //     System.out.println("T: " + CompareHelper.isEqual(E1.aa, E1.aa));
    //     System.out.println("F: " + CompareHelper.isEqual(E1.aa, E1.bb));
    //     System.out.println("F: " + CompareHelper.isEqual(null, E1.aa));
    //     System.out.println("F: " + CompareHelper.isEqual(E1.aa, null));
    //     System.out.println("T: " + CompareHelper.isEqual(e1, e2));
    //     System.out.println("F: " + CompareHelper.isEqual(e1, e3));
    //     System.out.println("F: " + CompareHelper.isEqual(e3, e4));
    //     System.out.println("F: " + CompareHelper.isEqual(e4, e3));
    //     System.out.println("T: " + CompareHelper.isEqual(e4, e5));
    // }
}
