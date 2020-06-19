package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.tools.CompareHelper;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MixerTracks {

    public static class Track {
        public final int source_id;
        public final float volume;
        private final String type;

        public Track(int decId, float volume) {
            this.source_id = decId;
            this.volume = volume;
            this.type = "stream";
        }

        public boolean isValid() {
            return source_id >= 0 && volume >= 0.0f && volume <= 1.0f;
        }

        public boolean isEqual(Track other) {
            if (other == null)
                return false;

            return source_id == other.source_id && volume == other.volume;
        }
    }

    public List<Track> tracks;

    public MixerTracks(List<Track> tracks) {
        Set<Track> set = new TreeSet<>(gComparator);
        set.addAll(tracks);
        this.tracks = new LinkedList<>(set);
    }

    public MixerTracks(MixerTracks other) {
        this.tracks = new LinkedList<>(other.tracks);
    }

    public boolean isEqual(MixerTracks other) {
        if (other == null)
            return false;

        return CompareHelper.isEqual(tracks, other.tracks, (src, dst) -> {
            if (tracks.size() != other.tracks.size())
                return false;
            for (int i = 0 ; i < tracks.size() ; ++i) {
                Track sc = tracks.get(i);
                Track dc = other.tracks.get(i);
                if (!sc.isEqual(dc))
                    return false;
            }
            return true;
        });
    }

    public boolean isValid() {
        if (tracks == null)
            return false;
        for (Track track: tracks) {
            if (!track.isValid())
                return false;
        }
        return true;
    }

    public boolean contains(int decId) {
        if (tracks == null)
            return false;
        for (Track track: tracks) {
            if (track.source_id == decId)
                return true;
        }
        return false;
    }

    public static MixerTracks buildEmpty() {
        return new MixerTracks(new LinkedList<>());
    }

    public static MixerTracks combine(MixerTracks s1, MixerTracks s2) {
        List<Track> tracks = new LinkedList<>();
        tracks.addAll(s1.tracks);
        tracks.addAll(s2.tracks);
        return new MixerTracks(tracks);
    }

    public static MixerTracks remove(MixerTracks in, List<Integer> decIds) {
        MixerTracks result = new MixerTracks(in);
        if (decIds.size() == 0)
            return result;

        Iterator<Track> it = result.tracks.iterator();
        while (it.hasNext()) {
            Track track = it.next();
            if(decIds.contains(track.source_id)) {
                it.remove();
            }
        }

        return result;
    }

    private static int compare(int s1, int s2) {
        return s1 == s2 ? 0 : (s1 > s2 ? 1 : -1);
    }

    private static final Comparator<Track> gComparator = (s1, s2) -> compare(s1.source_id, s2.source_id);
}
