package cn.sanbu.avalon.endpoint3.structures;

public enum RecFileFormat {
    MP4(".mp4"),
    FLV(".flv");

    public final String suffix;

    RecFileFormat(String suffix) {
        this.suffix = suffix;
    }

    public static RecFileFormat fromSuffix(String suffix) {
        if (suffix == null)
            return null;
        for (RecFileFormat strategy: RecFileFormat.values()) {
            if (suffix.equals(strategy.suffix))
                return strategy;
        }
        return null;
    }
}
