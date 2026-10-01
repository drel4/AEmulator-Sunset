package app.aemu.core

/** Decisions shared by boot paths, independent of Android services for regression tests. */
internal object SetupWizardOption {
    fun restoreSonyFlow(skip: Boolean, hasState: Boolean) = !skip && !hasState
    fun runHelper(skip: Boolean, hasState: Boolean) = skip || hasState
}
