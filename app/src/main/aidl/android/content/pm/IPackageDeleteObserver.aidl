package android.content.pm;

interface IPackageDeleteObserver {
    void packageDeleted(in String packageName, in int returnCode);
}
