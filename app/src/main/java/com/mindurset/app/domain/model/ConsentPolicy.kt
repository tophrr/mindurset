package com.mindurset.app.domain.model

/**
 * The one consent this build asks for before a check-in can be saved, and the policy text it refers
 * to. Bumping [VERSION] means the person is asked again, which is why it is a constant and not a
 * literal at the call site.
 */
object ConsentPolicy {
    const val KIND = "local_processing"
    const val VERSION = "1.0"
}
