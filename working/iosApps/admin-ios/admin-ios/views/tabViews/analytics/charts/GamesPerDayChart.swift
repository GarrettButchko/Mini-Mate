//
//  GamesPerDayChart.swift
//  MiniMate Manager
//
//  Created by Garrett Butchko on 3/8/26.
//

import Charts
import SwiftUI
import shared_admin

struct GamesPerDayChart: View {
    
    @EnvironmentObject var VM: OperationsViewModelSwift
    
    @State var data: [PlayerActivity] = []
    
    let lineColor: Color = .purple
    
    var body: some View {
        mainContent
            .task {
                await fetchData()
            }
    }
    
    @ViewBuilder
    private var mainContent: some View {
        if data.isEmpty || data.allSatisfy({ $0.count == 0 }) {
            emptyStateView
        } else {
            chartCardView
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 10) {
            Image(systemName: "chart.line.uptrend.xyaxis")
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(.secondary)
            Text("Not enough data yet")
                .font(.headline)
                .fontWeight(.semibold)
            Text("Games over time will appear here once players start visiting your course.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(RoundedRectangle(cornerRadius: 17).fill(.subTwo))
    }
    
    private var chartCardView: some View {
        VStack {
            HStack(alignment: .center) {
                Text("Games Over Time")
                    .foregroundStyle(.mainOpp)
                    .font(.system(size: 14, weight: .semibold))
                Spacer()
                InfoButton(infoText: "Shows the duration of the games over time.")
            }
            .padding(.bottom, 16)
            
            chartView
        }
        .frame(height: 240)
        .padding()
        .background {
            RoundedRectangle(cornerRadius: 17)
                .fill(.subTwo)
        }
        .id(VM.rangeDailyDocs.count)
    }
    
    private var chartView: some View {
        Chart {
            ForEach(data) { item in
                // Pre-calculating the date helps the compiler resolve Chart overloads faster
                let date = item.date.toNSDate()
                
                LineMark(
                    x: .value("Day", date, unit: .day),
                    y: .value("Games", item.count)
                )
                .foregroundStyle(lineColor)
                .lineStyle(StrokeStyle(lineWidth: 2, lineCap: .round))
                
                AreaMark(
                    x: .value("Day", date, unit: .day),
                    y: .value("Games", item.count)
                )
                .foregroundStyle(
                    .linearGradient(
                        colors: [lineColor.opacity(0.4), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            }
        }
        .chartPlotStyle { plot in
            plot
                .frame(maxHeight: .infinity, alignment: .top)
                .padding(.bottom, 8)
        }
        .chartYScale(domain: .automatic(includesZero: true))
        .chartYAxis {
            AxisMarks(position: .leading, values: .automatic(desiredCount: 5)) { value in
                AxisGridLine()
                AxisValueLabel()
            }
        }
        .chartXAxis {
            AxisMarks(preset: .aligned, values: getStrideValues(from: data)) { value in
                AxisGridLine()
                    .foregroundStyle(.secondary.opacity(0.3))
                
                AxisValueLabel {
                    if let date = value.as(Date.self) {
                        Text(date, format: .dateTime.month().day())
                    }
                }
            }
        }
    }
    
    private func fetchData() async {
        do {
            data = try await VM.getDataForGamesPerDay()
        } catch {
            print("Error fetching games per day data: \(error)")
        }
    }
    
    func getStrideValues(from data: [PlayerActivity]) -> [Date] {
        let count = data.count
        
        // Convert LocalDateTime to Date using the new extension
        let dates = data.map { $0.date.toNSDate() }
        
        if count <= 5 {
            return dates
        }
        
        guard let first = dates.first, let last = dates.last else { return [] }
        
        let diff = last.timeIntervalSince(first)
        let step = diff / 5
        
        return [
            first.addingTimeInterval(step),
            first.addingTimeInterval(step * 2),
            first.addingTimeInterval(step * 3),
            first.addingTimeInterval(step * 4)
        ]
    }
}
