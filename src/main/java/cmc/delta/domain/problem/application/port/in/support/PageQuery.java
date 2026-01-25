package cmc.delta.domain.problem.application.port.in.support;

/**
 * Spring Pageable 대체.
 * page는 0-base.
 */
public record PageQuery(int page, int size) {
}
