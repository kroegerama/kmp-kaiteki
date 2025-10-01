package com.kroegerama.kmp.kaiteki

import platform.UIKit.UIDevice

public actual object Platform {
    public actual val name: String = UIDevice.currentDevice.systemName + " " + UIDevice.currentDevice.systemVersion
}
