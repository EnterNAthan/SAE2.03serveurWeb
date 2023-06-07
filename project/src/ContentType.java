import java.util.List;

public enum ContentType {
    IMAGE(
            "image",
            List.of("png", "jpg", "jpeg", "png", "webp", "ico", "gif")
    ),
    VIDEO(
            "video",
            List.of("mp4", "webm")
    ),
    AUDIO(
            "audio",
            List.of("mp3", "wav", "ogg")
    );

    private final String type;
    private final List<String> extensions;

    ContentType(String type, List<String> extensions) {
        this.type = type;
        this.extensions = extensions;
    }

    public String getHeader(String extension) {
        return "Content-Type: " + type + "/" + extension + "\r\n";
    }

    public static ContentType of(String extension) {
        for (ContentType contentType : ContentType.values()) {
            for (String ext : contentType.extensions) {
                if (ext.equalsIgnoreCase(extension))
                    return contentType;
            }
        }

        return null;
    }
}
