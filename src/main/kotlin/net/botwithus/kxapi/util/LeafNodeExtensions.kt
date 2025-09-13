package net.botwithus.kxapi.util

import net.botwithus.xapi.script.BwuScript
import net.botwithus.xapi.script.permissive.node.LeafNode


internal fun leafNode(desc: String, script: BwuScript, action: () -> Unit): LeafNode =
    LeafNode(script, desc, Runnable { action() })

