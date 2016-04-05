package il.co.topq.report.front.scheduled;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import il.co.topq.report.Common;
import il.co.topq.report.Configuration;
import il.co.topq.report.Configuration.ConfigProps;
import il.co.topq.report.business.execution.MetadataController;
import il.co.topq.report.business.execution.MetadataController.ExecutionMetadata;
import il.co.topq.report.events.ExecutionUpdatedEvent;

@Component
public class HtmlReportsEraser {

	private final static Logger log = LoggerFactory.getLogger(HtmlReportsEraser.class);

	private int daysToKeep = 0;

	private boolean enabled;

	private final MetadataController executionManager;

	private final ApplicationEventPublisher publisher;

	@Autowired
	public HtmlReportsEraser(MetadataController executionManager, ApplicationEventPublisher publisher) {
		this.executionManager = executionManager;
		this.publisher = publisher;
		daysToKeep = Configuration.INSTANCE.readInt(ConfigProps.DAYS_TO_KEEP_HTML_REPORTS);
		if (daysToKeep > 0) {
			enabled = true;
		} else {
			log.debug("Html reports eraser is disabled");
		}
	}

	@Scheduled(fixedRate = 10000)
	public void eraseOldHtmlReports() {
		if (!enabled) {
			return;
		}
		log.trace("Waking up in order to search for HTML reports that need to be erased");
		final LocalDate today = LocalDate.now();
		final ExecutionMetadata[] metaDataArr = executionManager.getAllMetaData();
		for (ExecutionMetadata meta : metaDataArr) {
			if (meta.isActive() || meta.isLocked() || !meta.isHtmlExists()) {
				continue;
			}
			final LocalDate executionDate = LocalDate.parse(meta.getDate(),
					DateTimeFormatter.ofPattern(Common.API_DATE_FORMATTER.toPattern()));
			final long old = ChronoUnit.DAYS.between(executionDate, today);

			if (old > daysToKeep) {
				log.debug("Execution with id " + meta.getId() + " creation date is " + meta.getDate()
						+ " which makes it " + old + " days old which is more then the maximum of " + old
						+ " to keep. About to delete HTML reports of the execution");
				meta.setHtmlExists(false);
				publisher.publishEvent(new ExecutionUpdatedEvent(meta));
			}

		}

	}

}