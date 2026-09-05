# ViewModels are built by reflection on their constructor, so R8 must not strip it.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
	<init>(...);
}
