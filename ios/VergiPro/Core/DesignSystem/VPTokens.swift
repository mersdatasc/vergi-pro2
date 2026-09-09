import SwiftUI
import UIKit

enum VPColor {
    /// Dinamik ana marka rengi: Açık temada (#18181B - Zinc-900), Koyu temada saf beyaz (#FFFFFF)
    static let brand = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? .white : UIColor(red: 24/255, green: 24/255, blue: 27/255, alpha: 1.0)
    })

    /// Ters marka rengi: Açık temada beyaz, Koyu temada zinc-900
    static let brandInverse = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 24/255, green: 24/255, blue: 27/255, alpha: 1.0) : .white
    })

    /// Ana metin rengi
    static let textPrimary = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 250/255, green: 250/255, blue: 250/255, alpha: 1.0) : UIColor(red: 24/255, green: 24/255, blue: 27/255, alpha: 1.0)
    })

    /// Vurgu ve durum renkleri — Web arayüzü ile birebir uyumlu
    static let success = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 52/255, green: 211/255, blue: 153/255, alpha: 1.0) : UIColor(red: 5/255, green: 150/255, blue: 105/255, alpha: 1.0)
    })

    static let warning = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 251/255, green: 191/255, blue: 36/255, alpha: 1.0) : UIColor(red: 217/255, green: 119/255, blue: 6/255, alpha: 1.0)
    })

    static let danger = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 248/255, green: 113/255, blue: 113/255, alpha: 1.0) : UIColor(red: 220/255, green: 38/255, blue: 38/255, alpha: 1.0)
    })

    static let info = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 96/255, green: 165/255, blue: 250/255, alpha: 1.0) : UIColor(red: 37/255, green: 99/255, blue: 235/255, alpha: 1.0)
    })

    static let accentBlue = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 96/255, green: 165/255, blue: 250/255, alpha: 1.0) : UIColor(red: 37/255, green: 99/255, blue: 235/255, alpha: 1.0)
    })

    /// Arka plan tuvali (Canvas) — Açık modda Slate-50 (#F8FAFC), Koyu modda saf siyah (#000000)
    static let canvas = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? .black : UIColor(red: 248/255, green: 250/255, blue: 252/255, alpha: 1.0)
    })

    /// Kart yüzeyi (Surface) — Açık modda saf beyaz (#FFFFFF), Koyu modda derin koyu (#121214)
    static let surface = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 18/255, green: 18/255, blue: 20/255, alpha: 1.0) : .white
    })

    /// Kart kenarlık çizgisi (Border) — #E2E8F0 (Slate-200), Koyu modda zarif hat
    static let cardBorder = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(white: 1.0, alpha: 0.10) : UIColor(red: 226/255, green: 232/255, blue: 240/255, alpha: 1.0)
    })

    static let separator = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(white: 1.0, alpha: 0.08) : UIColor(red: 226/255, green: 232/255, blue: 240/255, alpha: 1.0)
    })

    static let secondaryText = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 161/255, green: 161/255, blue: 170/255, alpha: 1.0) : UIColor(red: 100/255, green: 116/255, blue: 139/255, alpha: 1.0)
    })

    static let tertiaryText = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 113/255, green: 113/255, blue: 122/255, alpha: 1.0) : UIColor(red: 148/255, green: 163/255, blue: 184/255, alpha: 1.0)
    })
}

enum VPSpace {
    static let xxs: CGFloat = 4
    static let xs: CGFloat = 8
    static let sm: CGFloat = 12
    static let md: CGFloat = 16
    static let lg: CGFloat = 24
    static let xl: CGFloat = 32
    static let xxl: CGFloat = 48
}

enum VPRadius {
    static let small: CGFloat = 8
    static let medium: CGFloat = 12
    static let card: CGFloat = 16
    static let large: CGFloat = 20
    static let hero: CGFloat = 28
    static let pill: CGFloat = 999
}

struct VPCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(VPSpace.md)
            .background(
                RoundedRectangle(cornerRadius: VPRadius.card, style: .continuous)
                    .fill(VPColor.surface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: VPRadius.card, style: .continuous)
                    .strokeBorder(VPColor.cardBorder, lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.02), radius: 6, x: 0, y: 2)
    }
}

extension View {
    func vpCard() -> some View { modifier(VPCardModifier()) }
}

// MARK: - Marka Bileşenleri

/// VP monogram ikonu — yuvarlak köşeli kart içinde veya serbest.
/// Light: beyaz zemin, siyah VP. Dark: siyah zemin, beyaz VP.
struct VPBrandMark: View {
    @Environment(\.colorScheme) private var colorScheme

    var size: CGFloat = 58
    var cornerRadius: CGFloat = 16
    var contained: Bool = true

    var body: some View {
        if contained {
            ZStack {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(VPColor.surface)
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .strokeBorder(VPColor.cardBorder, lineWidth: 1)
                    )
                Image("VPMark")
                    .resizable()
                    .interpolation(.high)
                    .scaledToFit()
                    .padding(size * 0.10)
            }
            .frame(width: size, height: size)
            .accessibilityHidden(true)
        } else {
            Image("VPMark")
                .resizable()
                .interpolation(.high)
                .scaledToFit()
                .frame(width: size, height: size)
                .accessibilityHidden(true)
        }
    }
}

/// "VergiPro" kelime markası — ikon solda, yazı sağda.
/// Nav bar, splash ve karşılama ekranlarında kullanılır.
struct VPWordmark: View {
    @Environment(\.colorScheme) private var colorScheme
    var iconSize: CGFloat = 32
    var fontSize: CGFloat = 22

    var body: some View {
        HStack(spacing: 10) {
            Image("VPMark")
                .resizable()
                .interpolation(.high)
                .scaledToFit()
                .frame(width: iconSize, height: iconSize)
            Text("VergiPro")
                .font(.system(size: fontSize, weight: .bold, design: .default))
                .tracking(-0.5)
                .foregroundStyle(.primary)
        }
        .accessibilityLabel("VergiPro")
    }
}

// MARK: - Global Button & Input Styles

struct VPPrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: 54)
            .foregroundStyle(VPColor.brandInverse)
            .background(VPColor.brand.opacity(configuration.isPressed ? 0.82 : 1), in: RoundedRectangle(cornerRadius: VPRadius.medium, style: .continuous))
            .scaleEffect(configuration.isPressed ? 0.985 : 1)
            .animation(.easeOut(duration: 0.14), value: configuration.isPressed)
    }
}

struct VPSecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: 54)
            .foregroundStyle(.primary)
            .background(VPColor.surface, in: RoundedRectangle(cornerRadius: VPRadius.medium, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: VPRadius.medium, style: .continuous).stroke(Color(uiColor: .separator), lineWidth: 1))
            .opacity(configuration.isPressed ? 0.72 : 1)
    }
}

extension View {
    func vpInput() -> some View {
        padding(.horizontal, VPSpace.md)
            .frame(minHeight: 54)
            .background(VPColor.surface, in: RoundedRectangle(cornerRadius: VPRadius.medium, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: VPRadius.medium, style: .continuous).stroke(Color(uiColor: .separator), lineWidth: 1))
    }
}

// MARK: - Currency Formatting Extensions (Rock-solid Turkish Lira formatters)

extension Double {
    var formattedTRY: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencySymbol = "₺"
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
        formatter.locale = Locale(identifier: "tr_TR")
        return formatter.string(from: NSNumber(value: self)) ?? String(format: "₺%.2f", self)
    }

    var formattedTRYCompact: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencySymbol = "₺"
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        formatter.locale = Locale(identifier: "tr_TR")
        return formatter.string(from: NSNumber(value: self)) ?? String(format: "₺%.0f", self)
    }
}

extension Int {
    var formattedTRYCompact: String {
        Double(self).formattedTRYCompact
    }
    var formattedTRY: String {
        Double(self).formattedTRY
    }
}
