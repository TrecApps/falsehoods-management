package com.trecapps.falsehoods.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WelcomeService {

    @Data
    @AllArgsConstructor
    public static class Guideline {
        String label;
        List<String> content;
    }

    List<Guideline> guidelines = List.of(
            new Guideline(
                    "Overview",
                    List.of("The goal of this program is to provide a robust means of factchecking various institutions" +
                                    " ranging from anchors on Fox News or MSNBC (or whatever their future name will be)" +
                                    " and Senator's in the halls of the US Congress to the various podcasts available " +
                                    "on YouTube or Twitch.",
                            "Misinformation has always been a problem in our society and it has arguable gotten worse" +
                                    " in recent decades and years. There may not be an optimal solution to this issue," +
                                    " but this service is meant as a possible solution.")
            ),
            new Guideline(
                    "Falsehood Types",
                    List.of("Because Falsehoods come in multiple forms, TrecApps is currently utilizing four categories of Falsehoods:",
                            "Objective Falsehoods: This is self-explainitory, but this type of falsehood is where a claim is made and it is objectively untrue (even if it may not be known to be untrue at the time the claim is made)",
                            "Omission: This is a case where a figure or reporter is pushing a narrative and they deliberately leave out crucial peices of information that could reasonably cause the audience to draw different conclusions than what was intended. Note: the figure could also have mentioned this crucial information but coded or hidden in such a way that they clearly intended for their audience to not notice)",
                            "Subjective Falsehood: This is a case where a figure set a standard in order to make a judgement but fails to apply that standard in another case or against another person/group for which that standard could be applied.",
                            "Faulty Logic: This is a case where a figure uses logic that doesn't make sense when scrutiny is applied.")
            ),
            new Guideline(
                    "Entry Lifecycle",
                    List.of("When you create and verify your TrecAccount, you start out with 5 points of credibility. You will need that credibility to submit a falsehood entry. When you do so, you should establish when the falsehood was made, who made it, what Media Outlet and or institution that figure represented, and other details. You then have to fill out your rationale demostrating what was said and how it was a falsehood. It is highly recommended that you play 'Devil's advocate'.",
                            "This platform is not a Social Media platform like Facebook, Twitter/X or even CoffeeShop - where you can surround yourself with connections/friends that laregly agree with what you say. What you say here will be scrutinized - this service (should it take off) will paint an unflttering picture of various public figures and institutions - we want to make sure we dot our i's and cross our t's.",
                            "Once you submit your entry, there is an initial round of review. If the reviewers at this stage accept your entry, it is moved to the 'ACCEPTED' stage.",
                            "Once accepted, there is a 3-month waiting period where 'briefs' can be applied to your entry. These briefs can be made to support an entry or to object to one. The purpose of this period is to",
                            "1. Give those implicated in the falsehood (or supporter of theirs) a chance to provide context to what was said or to call out shortcomings in the submission that the initial review missed. For instance, imagine that the submitter used AI to manufacture evidence that a figure made a falsehood and it gets through the initial round of review. This period is a chance for the usage of AI to come to light.",
                            "2. If a public figure/Media Outlet/Insitution is being called out but are not listed in our database, this gives TrecApps the opportunity to add the relevent entries to our database and update the submission accordingly before the final review.",
                            "After the 3 month period has passed, there is a second round of review. If the entry passes, then it is marked as 'CONFIRMED' and it officially counts against the Public Figure/Institution"
                    )
            ),
            new Guideline(
                    "Credibility and Restrictions",
                    List.of("A Credibility Rating is a means of assessing how reliable of a fact-checker you are and whether or not you are acting in good-faith. Each person, once verified, starts out with 5 points. Without a subscription, beginners are entitled to one submission per day to limit the number of sloppy entries a bad-faith actor can flood our databases with. You can enlarge this limitation based off of how many Credibility Points you earn or by subscribing to a Falsehood Subscription (To-Do: Add Details)"
                    )
            ),
            new Guideline(
                    "First Review",
                    List.of("Once Submitted, a Falsehood Entry becomes subject to review. To become an eligible reviewer at this stage, your credibility needs to reach 75 points",
                            "When looking over a submission, you will have four options:",
                            "Approve: you assert that it passes the bar and can proceed",
                            "Suggest: You believe that it almost meets your bar for approving it, and you mention a couple tweaks the user can make so that you do approve of it.",
                            "Reject: You do not believe that the submission withstands scrutiny and that it should be canned. You also think that the user did submit in good faith.",
                            "Penalize: Has the same effect on the submission as 'Reject' but with the caveat that the entry was sloppy or the submitter was acting in bad-faith and should loose credibility points over it.",
                            "Before the review is complete, the user has the option to make edit to the submission. Once a verdict is reached, edits are closed off.",
                            "A verdict is reached when 2/3rds of eligible reviewers (or 10 reviewers) reach a consensus. Note that 'Suggests' are not considered verdicts and do not count towards a full verdict. A positive consensus is when 2/3rds provide an 'Approve' verdict. If this happens, then the submission is 'APPROVED' and is given a 3-month period for briefs to be filed before the second review. You can think of this as an Indictment against whomever made the alleged Falsehood.",
                            "A Negative consensus is when 2/3rds of the reviewers have provided a 'Reject' or 'Penalize' verdict. If this happens, the submission is considered 'REJECTED'. If more reviewers have selected 'Reject' than Penalize on a 'REJECTED' entry, nothing happens to the user and they are free to try again. If more reviewers have selected 'Penalize' on a 'REJECTED' entry, then the submitter is autmatically deducted 5 Credibility Points.",
                            "There is an expectation that reviewers explain the reasoning behind their verdicts (not just site the guidelines, but discuss the contents of the submission itself."
                    )
            ),
            new Guideline(
                    "Briefing and the Approved Stage",
                    List.of("A Falsehood in the 'Approved' Stage must wait a minimum of 3-months before it can be reviewed again (Note that in Test environments, this perior may be much smaller). Think of this as a 'trial', there the 'defendent' (or his/her supporters) now has the opportunity to file 'briefs' opposing the submission.They can poke holes in the submission that the inital review stage may have missed. Other individuals have the opportunity to provide briefs affirming the submission (or pushing back against the claim).",
                            "A user needs to have 50 credibility points to file a brief (though the service should be updated to enable actual defendents to bypass this restriction). Briefs are also throttled, though this too can be expanded by additional credibility and subscriptions."
                    )
            ),
            new Guideline(
                    "Second Review",
                    List.of("To-Do: Expand on the functionality of this stage")
            ),
            new Guideline(
                    "Objective Falsehoods",
                    List.of("This type of Falsehood is arguably the most straight-forward but should still be offerred some guidance. Sometimes, the objective truth may not be known at the time the 'falsehood' was made. When attempting to submit an Objective Falsehood, ask the following questions.",
                            "1. Was there evidence strongly hinting at the claim that later evidence/reporting debunked?",
                            "2. Did the 'defendent' assert the claim as if it was definitevly true? (for instance, if a TV Reporter is discussing a Washington Post article with a faulty claim, did the reporter assert the claim to be true? Or just report the claim as asserted by the Washington Post?)",
                            "3. Is the claim coated in satire or hyperbole?",
                            "4. If discussing a quantitative claim that uses the value X, and you know the true value is Y, are they different enough that you can come to a different conclusion (or make a different choice) depending on which number you use?",
                            "To-Do: Ask additional questions"
                    )
            ),

            new Guideline(
                    "Omissions",
                    List.of("In court, there is a thing called lying by omission. One idea behind this is that not every falsehood is going to be as straight forward as the 'Objective Falsehood'. Here, the idea is that a reporter/pundit is trying to push a narrative and in order to get the audience to buy this narrative, he/she has to omit certain peices of information to accomplish this goal. Imagine you are on a jury and you have come to a verdict. Would this information have caused a different outcome had it been presented? If so, go ahead and try to submit the entry under this category.",
                            "Do consider whether the reporter did address some counter-arguments to the narrative that they were pushing. If these were compelling arguments, then perhaps it might be best not to make this submission."
                    )
            ),
            new Guideline(
                    "Subjective Falsehoods",
                    List.of("The idea behind this category is that sometimes, certain figures, groups, or parties get held to a standard that other parties largely get a pass for. Before making this submission, consider these questions:",
                            "1. When the reporter made the criticism against Party A, would it have been reasonable for the reporter ton know about Party B's cuplability?",
                            "2. Did the reporter make multiple criticisms against Party A and express how one or two could be overlooked?",
                            "3. Assuming that Party A is cuplable, is Party B close to being as culpable as Party A? Is Party B's culpability substantial enough that it would warrent scrutiny in the absence of Party A or in any other context?",
                            "4. Could the discrepency be the result of the reporter being a hack, or changing his/her views over time?",
                            "In short, this could be seen as clling out hypocracy."
                    )
            ),
            new Guideline(
                    "Faulty Logic",
                    List.of("All the other categories require some level of fact-checking beyond establishing what was said or reported. For this category, you just need to focus on what was said, assume that any objective claim they made was true, and how what was said doesn't hold up under scrutiny. For instance, assume a segmment has the following details:",
                            "* Assertion A",
                            "* Claim 1 that strongly supports Assertion A",
                            "* Claim 2 that loosely supports Assertion A",
                            "Assume that there exists 'Assertion B' that conflicts with Assertion A, is debunked by Claim 1 but not Claim 2.",
                            "If the segment contains Assertion A and both claims, then you don't have a case because Assertion B has been debunked.",
                            "If the segment contains Assertion A and Claim 2, but not Claim 1, you do have a case because Assertion B could still be made. Even if you could provide Claim 1, that was something that the reporter(s) behind the segment should have provided.",
                            "If the segment contains Assertion B and both claims, you do have a case because Claim 1 debunks the Assertion being made"
                    )
            ),
            new Guideline(
                    "Future Features",
                    List.of("Obviously, this site is new and could develop features over time. Features you can expect within the first year include:",
                            "1. An Appeals process: Useful in case a faulty entry makes it through both rounds of review or for contributers to call out bad-faith activites by reviewers (such as rejecting/penalizing entries without providing a good reason why)",
                            "2. A robust searching API to search falsehood based off of keyword and not just defendents",
                            "3. Fields in the falsehood entry to focus on targets of Falsehoods, rather than just the perpeatrators."
                    )
            )

    );


    public List<Guideline> getGuidelines(){
        return this.guidelines;
    }


}
