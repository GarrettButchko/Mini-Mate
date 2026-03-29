import Foundation

#if MINIMATE
import shared_user
#elseif MANAGER
import shared_admin
#endif

extension Firebase_firestoreTimestamp: @retroactive Comparable {
    public static func < (lhs: Firebase_firestoreTimestamp, rhs: Firebase_firestoreTimestamp) -> Bool {
        if lhs.seconds != rhs.seconds {
            return lhs.seconds < rhs.seconds
        }
        return lhs.nanoseconds < rhs.nanoseconds
    }

    public static func == (lhs: Firebase_firestoreTimestamp, rhs: Firebase_firestoreTimestamp) -> Bool {
        return lhs.seconds == rhs.seconds && lhs.nanoseconds == rhs.nanoseconds
    }
}
