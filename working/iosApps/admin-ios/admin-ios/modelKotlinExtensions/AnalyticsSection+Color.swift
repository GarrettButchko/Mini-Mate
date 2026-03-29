//
//  AnalyticsSection+Color.swift
//  admin-ios
//
//  Created by Garrett Butchko on 3/28/26.
//

import SwiftUI
import shared_admin

extension AnalyticsSection: @retroactive Identifiable {
    var color: Color {
        switch self {
        case .growth:
            return Color.green
        case .operations:
            return Color.purple
        case .experience:
            return Color.red
        case .retention:
            return Color.blue
        }
    }
    
    public var id: String { rawValue }
}

extension ChartTopic {
    var color: Color {
        switch self {
        case .total: return .purple
        case .first: return .blue
        case .returning: return .pink
        }
    }
}

extension Insight {
    var color: Color {
        switch insightType {
        case .good: return .green
        case .average: return .blue
        case .warning: return .orange
        case .critical: return .red
        }
    }
    
    var imageName: String {
        switch insightType {
        case .good: return "checkmark.circle.fill"
        case .average: return "exclamationmark.circle.fill"
        case .warning: return "exclamationmark.triangle.fill"
        case .critical: return "x.circle.fill"
        }
    }
}

extension HealthGrade {
    var color: Color {
        switch self {
        case .excellent, .great: return .green
        case .good, .satisfactory: return .blue
        case .fair, .needsImprovement: return .orange
        case .poor, .critical: return .red
        }
    }
}
    
