package pl.warlander.cdda.launcher.model.common;

import com.google.gson.annotations.SerializedName;

public enum LauncherResourceType {
    @SerializedName("none")
    NONE,
    @SerializedName("jar")
    JAR,
    @SerializedName("github")
    GITHUB,
    @SerializedName("download")
    DOWNLOAD;
}
