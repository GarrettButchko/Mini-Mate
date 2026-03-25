//
//  GameView.swift
//  MiniMate
//
//  Created by Garrett Butchko on 2/6/25.
//

import SwiftUI
import MapKit
import shared_user

// MARK: - CourseView

struct CourseView: View {
    @StateObject var courseVM = CourseViewModelSwift()
    @StateObject var courseSearch = CourseSearchViewModelSwift()
    
    let courseRepo = CourseRepository()
    
    var body: some View {
        GeometryReader { geometry in
            if courseSearch.hasLocationAccess {
                ZStack {
                    mapView
                    
                    VStack {
                        HStack {
                            Text("Course Search")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                                .padding()
                                .frame(height: 40)
                                .background(Material.regular, in: RoundedRectangle(cornerRadius: 25))
                                .cardShadow()
                            
                            Spacer()
                            
                            LocationButton()
                                .cardShadow()
                        }
                        
                        Spacer()
                        
                        if !courseSearch.isSearchPanelVisible {
                            CourseSearchButton()
                                .transition(.move(edge: .bottom).combined(with: .opacity))
                        } else {
                            VStack {
                                if courseSearch.selectedMapItem != nil {
                                    CourseResultView()
                                        .transition(.move(edge: .trailing).combined(with: .opacity))
                                } else {
                                    CourseSearchResultsView()
                                        .transition(.move(edge: .leading).combined(with: .opacity))
                                }
                            }
                            .background(Material.regular)
                            .clipShape(RoundedRectangle(cornerRadius: 25))
                            .cardShadow()
                            .frame(height: geometry.size.height * 0.4)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                            .animation(.spring, value: courseSearch.selectedMapItem)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 30)
                    .animation(.spring, value: courseSearch.isSearchPanelVisible)
                }
            } else {
                VStack {
                    Spacer()
                    Text("Please enable Location Services for this app.\n\nTap 'Open Settings' → Location → Allow While Using the App.")
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                    Button("Open Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .padding(.top, 8)
                    Spacer()
                }
                .frame(maxWidth: .infinity)
            }
        }
        .onAppear(perform: courseSearch.kotlin.onAppear)
        .environmentObject(courseVM)
        .environmentObject(courseSearch)
    }
    
    
    
    private var mapView: some View {
        Map(position: $courseSearch.mapCameraPosition, selection: $courseSearch.selectedMapItem) {
            ForEach(courseSearch.mapItems, id: \.self) { item in
                let name = item.name ?? "Unknown"
                let isSupported = courseSearch.nameExists[name] ?? false
                
                if #available(iOS 26.0, *) {
                    Marker(name, coordinate: item.location.coordinate)
                        .tint(isSupported ? .purple : .green)
                } else {
                    Marker(name, coordinate: item.placemark.coordinate)
                        .tint(isSupported ? .purple : .green)
                }
            }
            UserAnnotation()
        }
        .mapControls {
            MapCompass().mapControlVisibility(.hidden)
        }
        .onChange(of: courseSearch.selectedMapItem) { _, newItem in
            Task {
                // 1. Always update position first (even if newItem is nil)
                let dto = newItem?.toDTO()
                if let dto {
                    courseVM.kotlin.updatePosition(mapItem: dto)
                }
                
                guard let selectedMapItem = newItem else {
                    // Reset if selection is cleared
                    await MainActor.run {
                        courseVM.selectedCourse = nil
                    }
                    return
                }

                // 2. Attempt to fetch from the repository
                var foundCourse: Course? = nil
                
                if let name = selectedMapItem.name {
                    do {
                        foundCourse = try await courseRepo.fetchCourseByName(name: name)
                    } catch {
                        print("Fetch failed for \(name): \(error.localizedDescription)")
                    }
                }

                // 3. Logic: If fetch returned a course, use it.
                // Otherwise, convert the MapItem to a Course object.
                await MainActor.run {
                    if let fetched = foundCourse {
                        courseVM.selectedCourse = fetched
                    } else {
                        courseVM.selectedCourse = selectedMapItem.toCourse(isSupported: false)
                    }
                }
            }
        }
        .animation(.spring, value: courseSearch.mapCameraPosition)
    }
}

private struct LocationButton: View {
    @EnvironmentObject var viewModel: CourseSearchViewModelSwift
    var body: some View {
        Button{
            viewModel.kotlin.setNewMapPosition()
        } label: {
            ZStack {
                Circle()
                    .fill(Material.regular)
                    .frame(width: 40, height: 40)
                
                Image(systemName: "location.fill")
                    .resizable()
                    .foregroundColor(.primary)
                    .frame(width: 20, height: 20)
            }
        }
    }
}

// MARK: - Placeholder View Modifier
// This is a placeholder for your custom .cardShadow() modifier
private struct CardShadow: ViewModifier {
    func body(content: Content) -> some View {
        content.shadow(color: Color.black.opacity(0.15), radius: 8, x: 0, y: 4)
    }
}

private extension View {
    func cardShadow() -> some View {
        self.modifier(CardShadow())
    }
}

