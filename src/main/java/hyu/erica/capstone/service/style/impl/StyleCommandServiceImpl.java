package hyu.erica.capstone.service.style.impl;

import hyu.erica.capstone.api.code.status.ErrorStatus;
import hyu.erica.capstone.api.exception.GeneralException;
import hyu.erica.capstone.client.PlanClient;
import hyu.erica.capstone.domain.Restaurant;
import hyu.erica.capstone.domain.Style;
import hyu.erica.capstone.domain.TripPlan;
import hyu.erica.capstone.domain.User;
import hyu.erica.capstone.domain.enums.City;
import hyu.erica.capstone.domain.mapping.PreferRestaurant;
import hyu.erica.capstone.repository.AttractionRepository;
import hyu.erica.capstone.repository.PreferAttractionRepository;
import hyu.erica.capstone.repository.PreferRestaurantRepository;
import hyu.erica.capstone.repository.RestaurantRepository;
import hyu.erica.capstone.repository.StyleRepository;
import hyu.erica.capstone.repository.TripPlanRepository;
import hyu.erica.capstone.repository.UserRepository;
import hyu.erica.capstone.service.style.StyleCommandService;
import hyu.erica.capstone.web.dto.client.RestaurantRequestDTO;
import hyu.erica.capstone.web.dto.style.request.UserStyleRequestDTO;
import hyu.erica.capstone.web.dto.style.response.UserStyleFinalResponseDTO;
import hyu.erica.capstone.web.dto.style.response.UserStyleInitResponseDTO;
import hyu.erica.capstone.web.dto.style.response.UserStyleResponseDTO;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StyleCommandServiceImpl implements StyleCommandService {

    private final UserRepository userRepository;
    private final StyleRepository styleRepository;
    private final RestaurantRepository restaurantRepository;
    private final AttractionRepository attractionRepository;
    private final TripPlanRepository tripPlanRepository;

    private final PreferRestaurantRepository preferRestaurantRepository;
    private final PreferAttractionRepository preferAttractionRepository;

    private final PlanClient planClient;

    private final static String TITLE = "여행 계획";
    private final static String PROFILE_IMAGE = "https://i.imgur.com/3zX2Z1b.png";

    @Override
    public UserStyleInitResponseDTO initStyle(Long userId) {
        User user = userRepository.findById(userId).orElseThrow( () -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        Style style = Style.builder()
                .city(City.BUSAN)
                .user(user)
                .build();
        Style save = styleRepository.save(style);

        return UserStyleInitResponseDTO.of(userId, save.getId());
    }

    @Override
    public UserStyleResponseDTO updateStyle(Long userId, Long styleId, UserStyleRequestDTO request) {
        Style style = styleRepository.findById(styleId).orElseThrow( () -> new GeneralException(ErrorStatus._STYLE_NOT_FOUND));

        if (!Objects.equals(style.getUser().getId(), userId))
            throw new GeneralException(ErrorStatus._UNAUTHORIZED_USER);

        style.updateStyle(request.startDate(), request.endDate(), request.preferActivity(), request.requirement());

        Style save = styleRepository.save(style);

        return UserStyleResponseDTO.of(save);
    }

    @Override
    public UserStyleFinalResponseDTO submitStyle(Long styleId, Long userId) {
        Style style = styleRepository.findById(styleId).orElseThrow( () -> new GeneralException(ErrorStatus._STYLE_NOT_FOUND));
        User user = userRepository.findById(userId).orElseThrow( () -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        StringBuilder sb = new StringBuilder();
        sb.append("여행 지역 : ").append(style.getCity().name()).append("\n")
                .append("시작 날짜 : ").append(style.getStartDate()).append("\n")
                .append("종료 날짜 : ").append(style.getEndDate()).append("\n")
                .append("선호 활동 : ").append(style.getPreferActivity()).append("\n")
                .append("추가 요구 사항 : ").append(style.getRequirement());
//
//        AttractionRequestDTO attractions = planClient.getAttractions(sb.toString());
        RestaurantRequestDTO restaurants = planClient.getRestaurants(sb.toString());

        TripPlan tripPlan = TripPlan.builder()
                .user(user)
                .startDate(style.getStartDate())
                .endDate(style.getEndDate())
                .title(TITLE)
                .profileImage(PROFILE_IMAGE)
                .build();


        List<Long> restaurantIds = restaurants.restaurant_ids();

        for (Long restaurantId : restaurantIds) {
            Restaurant restaurant =  restaurantRepository.findById(restaurantId).orElseThrow(
                    () -> new GeneralException(ErrorStatus._RESTAURANT_NOT_FOUND));
            preferRestaurantRepository.save(PreferRestaurant.builder()
                    .restaurant(restaurant)
                    .user(user)
                    .isPrefer(true)
                    .tripPlan(tripPlan)
                    .build());
        }

//        List<Long> attractionIds = attractions.attraction_ids();
//        for (Long attractionId : attractionIds) {
//            Attraction attraction = attractionRepository.findById(attractionId).orElseThrow(
//                    () -> new GeneralException(ErrorStatus._ATTRACTION_NOT_FOUND));
//            preferAttractionRepository.save(PreferAttraction.builder()
//                    .attraction(attraction)
//                    .user(user)
//                    .build());
//        }

        TripPlan save = tripPlanRepository.save(tripPlan);

        return UserStyleFinalResponseDTO.of(restaurants.restaurant_ids(), List.of(), save.getId());
    }
}
