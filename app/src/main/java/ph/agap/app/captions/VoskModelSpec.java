package ph.agap.app.captions;

/** One Vosk speech model bundled in the APK: where it is, how big, which language. */
public final class VoskModelSpec {

    /** Filipino (contract 1.3): vosk-model-tl-ph-generic-0.6, 329 MB zipped, 776 MB unpacked, CC BY-NC-SA 4.0. */
    public static final VoskModelSpec FILIPINO = new VoskModelSpec(
            "vosk-model-tl-ph-generic-0.6", "fil-PH", 776_000_000L, 1_000_000_000L);

    /**
     * English fallback (contract 1.3, D-014): vosk-model-small-en-us-0.15, 41 MB zipped, 71 MB unpacked,
     * Apache-2.0. Used only where Google's offline English recognizer is not available.
     */
    public static final VoskModelSpec ENGLISH = new VoskModelSpec(
            "vosk-model-small-en-us-0.15", "en-US", 71_000_000L, 300_000_000L);

    /** The model's name and version; the asset is "<name>.zip", the marker holds this text. */
    public final String name;
    /** BCP 47 tag of the language it recognises. */
    public final String languageTag;
    /** What the unpacked model takes on disk, for the setup message. */
    public final long unpackedBytes;
    /** Free space wanted before unpacking (the unpacked size plus headroom). */
    public final long requiredFreeBytes;

    private VoskModelSpec(String name, String languageTag, long unpackedBytes, long requiredFreeBytes) {
        this.name = name;
        this.languageTag = languageTag;
        this.unpackedBytes = unpackedBytes;
        this.requiredFreeBytes = requiredFreeBytes;
    }

    /** The zip's name in the APK's assets. */
    public String asset() {
        return name + ".zip";
    }
}
