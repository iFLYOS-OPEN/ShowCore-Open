package android.content.pm;

interface IPackageInstallObserver {
    void packageInstalled(in String packageName, int returnCode);
}
