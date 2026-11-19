package com.alessiodp.libby;

public abstract class LibraryManager {
    public void addMavenCentral() {}
    public void addJitPack() {}
    public void addRepository(String url) {}
    public void loadLibrary(Library library) {}
    public void loadLibraries(Library... libraries) {}
}
