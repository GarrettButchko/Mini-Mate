//
//  SearchResultRow.swift
//  MiniMate
//
//  Created by Garrett Butchko on 2/27/25.
//

import SwiftUI
import MarqueeText
import MapKit
import shared_user

// MARK: - Search Results View
struct CourseSearchResultsView: View {
    @EnvironmentObject var courseViewModel: CourseViewModelSwift
    @EnvironmentObject var locationHandler: LocationHandlerSwift
    @State private var showRetryButton = false
    @State var titleHeight: CGFloat = 30
    
    // Platform-agnostic business logic isolated for KMP
    private let logic = SearchResultBusinessLogic()
    
    var body: some View {
        ZStack {
            if courseViewModel.isLoadingCourses || locationHandler.mapItems.isEmpty {
                VStack(spacing: 15) {
                    ProgressView()
                        .scaleEffect(1.5)
                    Text("Trying to find nearby courses...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    if showRetryButton {
                        Button(action: {
                            courseViewModel.kotlin.searchNearby()
                        }) {
                            Text("Try Again")
                                .fontWeight(.semibold)
                                .padding(.horizontal, 30)
                                .padding(.vertical, 10)
                                .background(Color.blue)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .onAppear {
                    showRetryButton = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + logic.retryButtonDelay) {
                        withAnimation(){
                            showRetryButton = true
                        }
                    }
                }
            } else {
                ScrollView {
                    VStack(alignment: .leading) {
                        if let userCoord = locationHandler.userLocation?.coordinate {
                            ForEach(locationHandler.mapItems, id: \.self) { mapItem in
                                if locationHandler.mapItems.count > 0 && mapItem != locationHandler.mapItems[0]{
                                    Divider()
                                }
                                SearchResultRow(item: mapItem, userLocation: userCoord)
                            }
                        } else {
                            Text("Fetching location...")
                        }
                    }
                    Rectangle()
                        .fill(.clear)
                        .frame(height: 4)
                }
                .mask(
                    VStack(spacing: 0){
                        // 1. The 40pt fade-in area
                        LinearGradient(
                            colors: [.clear, .black],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(height: titleHeight)
                        
                        Rectangle()
                            .fill(.black)
                    }
                )
                .contentMargins(.horizontal, 16)
                .contentMargins(.top, 54)
                .scrollContentBackground(.hidden)
                .background(Color.clear)
                
            }
            
            VStack{
                HStack {
                    Text("Courses")
                        .font(.title3).fontWeight(.bold)
                        .foregroundStyle(.mainOpp)
                    Spacer()
                    Button {
                        courseViewModel.kotlin.cancel()
                    } label: {
                        Text("Cancel")
                            .frame(width: 70, height: 30)
                            .background(.blue)
                            .foregroundStyle(.white)
                            .clipShape(Capsule())
                    }
                }
                .padding()
                .padding(.bottom, 16)
                .background {
                    GeometryReader { proxy in
                        Color.clear
                            .task(id: proxy.size) {
                                titleHeight = proxy.size.height // Capture the size and monitor changes
                            }
                    }
                }
                Spacer()
            }
        }
        .cardShadow()
    }
}



struct SearchResultRow: View {
    @EnvironmentObject var VM: CourseViewModelSwift
    @EnvironmentObject var locationHandler: LocationHandlerSwift
    
    @State private var isSupported: Bool = false
    
    let item: MKMapItem
    
    let userLocation: CLLocationCoordinate2D
    let courseRepo = CourseRepository()
    
    @State var fetchedCourse: Course? = nil
    
    // Platform-agnostic business logic isolated for KMP
    private let logic = SearchResultBusinessLogic()
    
    var course: Course {
        if let fetchedCourse {
            return fetchedCourse
        } else {
           return item.toCourse(isSupported: isSupported)
        }
    }
    
    var body: some View {
        Button {
            VM.kotlin.updatePosition(mapItem: item.toDTO())
            VM.kotlin.setCourse(course: course)
        } label: {
            HStack{
                VStack(alignment: .leading) {
                    
                    MarqueeText(
                        text: "\(course.name)",
                        font: UIFont.preferredFont(forTextStyle: .headline),
                        leftFade: 16,
                        rightFade: 16,
                        startDelay: 3
                    )
                    .foregroundStyle(.mainOpp)
                

                    let distanceInMiles = logic.metersToMiles(meters: distanceInMeters())
                    let address = VM.kotlin.getPostalAddress(mapItem: item.toDTO())
                
                    MarqueeText(
                        text: logic.buildSubtitle(distanceInMiles: distanceInMiles, address: address),
                        font: UIFont.preferredFont(forTextStyle: .subheadline),
                        leftFade: 16,
                        rightFade: 16,
                        startDelay: 4
                    )
                    .foregroundStyle(.mainOpp)
                }
                .frame(height: 50)
                Spacer()
                
                if course.isSupported{
                    ZStack{
                        Circle()
                            .fill(.purple.opacity(0.3))
                            .frame(width: 24, height: 24)
                        
                        Image("logo_svg")
                            .resizable()
                            .renderingMode(.template)
                            .scaledToFit()
                            .foregroundStyle(.mainOpp)
                            .frame(width: 17, height: 17)
                    }
                }
            }
        }
        .onAppear(){
            Task{
                await preloadNameChecks()
            }
        }
        .onChange(of: item) { _, _ in
            Task{
                await preloadNameChecks()
            }
        }
    }
    
    func preloadNameChecks() async {
        if let name = item.name {
            do {
                let course = try await courseRepo.fetchCourseByName(name: name)
                fetchedCourse = course
            } catch{
                print("Preload failed for \(name): \(error.localizedDescription)")
            }
        }
    }
    
    func distanceInMeters() -> Double {
        let offsetLat = logic.getOffsetLatitude(baseLatitude: userLocation.latitude)
        
        var distanceInMeters: Double
        
        
        if #available(iOS 26.0, *) {
            distanceInMeters = CLLocation(latitude: offsetLat, longitude: userLocation.longitude)
                .distance(from: CLLocation(latitude: item.location.coordinate.latitude,
                                           longitude: item.location.coordinate.longitude))
        } else {
            distanceInMeters = CLLocation(latitude: offsetLat, longitude: userLocation.longitude)
                .distance(from: CLLocation(latitude: item.placemark.coordinate.latitude,
                                           longitude: item.placemark.coordinate.longitude))
        }
        return distanceInMeters
    }
}
