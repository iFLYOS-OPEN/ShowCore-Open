package android.content.pm;

interface IPackageDataObserver {
    oneway void onRemoveCompleted(in String packageName, boolean succeeded);
}
