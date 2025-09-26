package net.botwithus.kxapi.util

import net.botwithus.rs3.entities.LocalPlayer
import net.botwithus.rs3.vars.VarDomain

fun LocalPlayer.inCombat(): Boolean = VarDomain.getVarBitValue(1899) == 1