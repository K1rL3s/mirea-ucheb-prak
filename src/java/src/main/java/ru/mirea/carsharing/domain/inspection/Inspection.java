package ru.mirea.carsharing.domain.inspection;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record Inspection(int id, String stage, LocalDateTime createdAt, List<Photo> photos) {
    public List<String> missingViews(List<PhotoView> views) {
        Set<Integer> taken = new HashSet<>();
        for (var photo : photos) {
            taken.add(photo.view().id());
        }
        return views.stream()
                .filter(view -> view.required() && !taken.contains(view.id()))
                .map(PhotoView::name)
                .toList();
    }
}
