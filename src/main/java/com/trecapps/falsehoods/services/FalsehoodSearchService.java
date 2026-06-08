package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class FalsehoodSearchService {

    @Autowired
    MongoRepo mongoRepo;

    @Autowired
    IObjectStorageService objectStorageService;

    List<BrandSearchResult> getBrands(List<UUID> ids, SortedSet<BrandSearchResult> brands){
        return new TreeSet<>(brands)
                .stream()
                .filter((BrandSearchResult b) -> {
                    return ids.contains(b.getBrand().getId());
                })
                .toList();
    }

    public Mono<FalsehoodFull> getFalsehood(UUID userId, UUID falsehoodId){
        return this.mongoRepo.retrieveFalsehood(falsehoodId)
                .flatMap((Optional<FalsehoodDocument> o) -> {
                    if(o.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "");
                    FalsehoodDocument falsehood = o.get();

                    if(FalsehoodStage.SAVED.equals(falsehood.getStatus()) && (
                            userId == null || !userId.equals(falsehood.getUCreator())
                            ))
                    {
                        // ToDo - raise alert somehow as this should not happen
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "");
                    }

                    return objectStorageService.getFalsehoodContent(falsehoodId)
                            .doOnNext((SortedSet<ContentVersion> content) -> {
                                if(userId == null || !userId.equals(falsehood.getUCreator())) {
                                    while(content.size() > 1)
                                        content.removeFirst();
                                }
                            })
                            .map((SortedSet<ContentVersion> content) -> {
                                FalsehoodFull full = new FalsehoodFull();
                                full.setMetadata(falsehood);
                                full.setContent(content);
                                return full;
                            })
                            .flatMap((FalsehoodFull full) -> {
                                return mongoRepo.findAllRecordsByFalsehoodId(falsehoodId)
                                        .collectList()
                                        .map((List<FalsehoodRecord> fRecords) -> {
                                            full.getMetadata().getRecords().addAll(fRecords);
                                            return full;
                                        });
                            })
                            .flatMap((FalsehoodFull full) -> {
                                return mongoRepo.retrieveBriefsByFalsehood(falsehoodId)
                                        .collectList()
                                        .map((List<Brief> briefs) -> {
                                            full
                                                    .getBriefs()
                                                    .addAll(
                                                            briefs
                                                                    .stream()
                                                                    .map(Brief::toReturnableObj)
                                                                    .toList()
                                                    );
                                            return full;
                                        });
                            });

                })
                .flatMap((FalsehoodFull full) -> {
                    FalsehoodDocument doc = full.getMetadata();
                    SortedSet<UUID> brandIds = new TreeSet<>();
                    brandIds.addAll(doc.getTargets());
                    brandIds.addAll(doc.getCulprits());
                    return this.mongoRepo.getBrandsByList(brandIds)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .flatMap((Brand brand) -> {
                                return this.objectStorageService.retrieveThumbnail(brand.getId())
                                        .map((String thumbnail) -> {
                                            BrandSearchResult complete = new BrandSearchResult();
                                            complete.setImage(thumbnail);
                                            complete.setBrand(brand);
                                            return complete;
                                        })
                                        .switchIfEmpty(Mono.just(brand)
                                                .map((Brand b) -> {
                                                    BrandSearchResult complete = new BrandSearchResult();
                                                    complete.setBrand(b);
                                                    return complete;
                                                }));
                            })
                            .collectList()
                            .map((List<BrandSearchResult> brands) -> {
                                SortedSet<BrandSearchResult> brandSet = new TreeSet<>(Comparator.comparing((BrandSearchResult complete) -> complete.getBrand().getId()));
                                FalsehoodRet ret1 = new FalsehoodRet();
                                brandSet.addAll(brands);


                                ret1.setAuthorDisplayName(doc.getAuthorDisplayName());
                                ret1.setCreator(doc.getCreator());
                                ret1.setUCreator(doc.getUCreator());
                                ret1.setId(doc.getId());
                                ret1.setDateMade(doc.getDateMade());

                                ret1.setNotes(doc.getNotes());
                                ret1.setSeverity(doc.getSeverity());
                                ret1.setTitle(doc.getTitle());
                                ret1.setShowBrand(doc.getShowBrand());

                                ret1.setCulprits(getBrands(doc.getCulprits(), brandSet));
                                ret1.setTargets(getBrands(doc.getTargets(), brandSet));

                                ret1.setRecords(doc.getRecords());
                                ret1.setStatus(doc.getStatus());

                                full.setMetadata(null);
                                full.setFullMetaData(ret1);
                                return full;
                            });
                });
    }

    Mono<FalsehoodQueryResult> searchAggregation(Aggregation aggregation){
        return mongoRepo.searchFalsehoods(aggregation)
                .flatMap((FalsehoodQueryDocuments docAndCount) -> {

                    List<FalsehoodDocument> docs = docAndCount.getResults();

                    SortedSet<UUID> brandIds = new TreeSet<>();
                    for(FalsehoodDocument doc: docs){
                        brandIds.addAll(doc.getCulprits());
                        brandIds.addAll(doc.getTargets());
                    }

                    return this.mongoRepo.getBrandsByList(brandIds)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .flatMap((Brand brand) -> {
                                return this.objectStorageService.retrieveThumbnail(brand.getId())
                                        .map((String thumbnail) -> {
                                            BrandSearchResult complete = new BrandSearchResult();
                                            complete.setImage(thumbnail);
                                            complete.setBrand(brand);
                                            return complete;
                                        }).switchIfEmpty(Mono.just(brand)
                                                .map((Brand b) -> {
                                                    BrandSearchResult complete = new BrandSearchResult();
                                                    complete.setBrand(b);
                                                    return complete;
                                                }));
                            })
                            .collectList()
                            .map((List<BrandSearchResult> brands) -> {
                                List<FalsehoodRet> ret = new ArrayList<>(docs.size());
                                SortedSet<BrandSearchResult> brandSet = new TreeSet<>(Comparator.comparing((BrandSearchResult complete) -> complete.getBrand().getId()));
                                brandSet.addAll(brands);

                                for(FalsehoodDocument doc: docs){
                                    FalsehoodRet ret1 = new FalsehoodRet();


                                    ret1.setAuthorDisplayName(doc.getAuthorDisplayName());
                                    ret1.setCreator(doc.getCreator());
                                    ret1.setUCreator(doc.getUCreator());
                                    ret1.setId(doc.getId());
                                    ret1.setDateMade(doc.getDateMade());

                                    ret1.setNotes(doc.getNotes());
                                    ret1.setSeverity(doc.getSeverity());
                                    ret1.setTitle(doc.getTitle());
                                    ret1.setShowBrand(doc.getShowBrand());
                                    ret1.setStatus(doc.getStatus());

                                    ret1.setCulprits(getBrands(doc.getCulprits(), brandSet));
                                    ret1.setTargets(getBrands(doc.getTargets(), brandSet));

                                    ret.add(ret1);
                                }

                                return ret;
                            })
                            .map((List<FalsehoodRet> rets) -> {
                                FalsehoodQueryResult ret = new FalsehoodQueryResult();
                                ret.setResults(rets);
                                ret.setTotalCount(docAndCount.getTotalCount());
                                return ret;
                            });

                });
    }

    public Mono<FalsehoodQueryResult> searchFalsehoods(
            UUID userId,
            FalsehoodStage status,
            int page,
            int size
    ){
        Criteria criteria = Criteria.where("_uCreator").is(userId);
        if(status != null){
            criteria = new Criteria().andOperator(
                    criteria,
                    Criteria.where("status").is(status)
            );
        }

        MatchOperation matchOperation = new MatchOperation(criteria);
        FacetOperation facetOperation = facet(
                Aggregation.skip((long) page * size),
                Aggregation.limit(size)
        ).as("results")
                .and(
                        count().as("count")
                ).as("totalCount");

        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation,
                facetOperation
        );

        return searchAggregation(aggregation);
    }



    public Mono<FalsehoodQueryResult> searchFalsehoods(
            FalsehoodSearch search,
            FalsehoodStage status,
            int page,
            int size
    ){
        MatchOperation matchOperation = new MatchOperation(search.generateCriteria(status));

        FacetOperation facetOperation = facet(
                Aggregation.skip((long) page * size),
                Aggregation.limit(size)
        ).as("results")
                .and(
                        count().as("count")
                ).as("totalCount");

        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation,
                facetOperation
        );


        return searchAggregation(aggregation);
    }

    MatchOperation getMatchOp(UUID brand, boolean isCulprit, FalsehoodStage stage){
        List<Criteria> criteriaList = new ArrayList<>(2);

        criteriaList.add(Criteria.where(isCulprit ? "culprits" : "targets").is(brand));
        criteriaList.add(Criteria.where("status").is(stage));
        return new MatchOperation(new Criteria().andOperator(criteriaList));
    }

    public Mono<BrandFalsehoodTable> searchCountByBrand(UUID brand){
        MatchOperation acceptedCulprit = getMatchOp(brand, true, FalsehoodStage.ACCEPTED);
        MatchOperation confirmedCulprit = getMatchOp(brand, true, FalsehoodStage.CONFIRMED);
        MatchOperation acceptedTarget = getMatchOp(brand, false, FalsehoodStage.ACCEPTED);
        MatchOperation confirmedTarget = getMatchOp(brand, false, FalsehoodStage.CONFIRMED);

        GroupOperation groupOperation = group("severity").count().as("count");
        ProjectionOperation projectOp = project("count").and("severity").previousOperation();

        FacetOperation operation = facet(
                        acceptedCulprit, groupOperation, projectOp
                ).as("acceptedCulpritCount")
                .and(
                        confirmedCulprit, groupOperation, projectOp
                ).as("confirmedCulpritCount")
                .and(
                        acceptedTarget, groupOperation, projectOp
                ).as("acceptedTargetCount")
                .and(
                        confirmedTarget, groupOperation, projectOp
                ).as("confirmedTargetName");

        return this.mongoRepo.searchFalsehoodCountByBrand(Aggregation.newAggregation(operation))
                .map(BrandFalsehoodTable::new);

    }
}
