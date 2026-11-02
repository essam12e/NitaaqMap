package com.nitaaq.map.core.domain

/** Validates input locally (fast feedback) then asks the server to sign a temporary link. */
class CreateTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(rawInput: String): TripResult =
        when (val check = MapsLinkValidator.check(rawInput)) {
            LinkCheck.Empty -> TripResult.Failure(TripError.EmptyLink)
            LinkCheck.Invalid -> TripResult.Failure(TripError.InvalidLink())
            is LinkCheck.Valid -> repository.createTrip(check.normalizedUrl)
        }
}
