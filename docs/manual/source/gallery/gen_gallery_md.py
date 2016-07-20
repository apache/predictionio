import yaml
import sys
import urllib.parse as urlparse

INTRO = \
"""\
---
title: Engine Template Gallery
---
"""

UNSUPERVISED = \
"""
<div id='unsupervised-learning'/>

## Unsupervised Learning
"""
CLASSIFICATION = \
"""
<div id='classification'/>

## Classification
"""

REGRESSION = \
"""
<div id='regression'/>

## Regression
"""

RECOMMENDER_SYSTEMS = \
"""
<div id='recommenders'/>

## Recommender Systems
"""

NLP = \
"""
<div id='nlp'/>

## Natural Language Processing
"""

TEMPLATE_INTRO = \
"""#### ***[{name}]({repo})***  """

STAR_BUTTON = \
"""
<iframe src="https://ghbtns.com/github-btn.html?user={user}&repo={repo}&\
type=star&count=true" frameborder="0" align="middle" scrolling="0" width="170px" height="20px"></iframe>

"""

TEMPLATE_DETAILS = \
"""
{description}

Type | Language | License | Status | PIO min version
:----: | :-----:| :-----: | :----: | :-------------:
{type} | {language} | {license} | {status} | {pio_min_version}

"""

SECTION_SEPARATOR = \
"""
<br/>
"""

class Template:

    def __init__(self, engine):
        for key, val in engine.items():
            setattr(self, key, val)

        self.tags = list(map(lambda s: s.lower(), self.tags))
        self.has_github = True if self.parse_github() else False

    def parse_github(self):
        pr = urlparse.urlparse(self.repo)
        if pr.netloc == 'github.com':
            path = pr.path.split('/')
            assert(len(path) >= 3)
            self.github_user = path[1]
            self.github_repo = path[2]
            return True
        else:
            return False

def write_template(mdfile, template):
    intro = TEMPLATE_INTRO.format(
                name=template.name,
                repo=template.repo)
    if template.has_github:
        intro += STAR_BUTTON.format(
                    user=template.github_user,
                    repo=template.github_repo)
    mdfile.write(intro)
    mdfile.write(TEMPLATE_DETAILS.format(
            description=template.description,
            type=template.type,
            language=template.language,
            license=template.license,
            status=template.status,
            pio_min_version=template.pio_min_version,
        ))

def write_templates(mdfile, templates):
    for t in templates:
        write_template(mdfile, t)

def write_markdown(mdfile, templates):
    classification = [engine for engine in templates if "classification" in engine.tags]
    regression = [engine for engine in templates if "regression" in engine.tags]
    unsupervised = [engine for engine in templates if "unsupervised" in engine.tags]
    recommenders = [engine for engine in templates if "recommender" in engine.tags]
    nlps = [engine for engine in templates if "nlp" in engine.tags]

    mdfile.write(INTRO)

    mdfile.write(CLASSIFICATION)
    write_templates(mdfile, classification)

    mdfile.write(SECTION_SEPARATOR)

    mdfile.write(REGRESSION)
    write_templates(mdfile, regression)

    mdfile.write(SECTION_SEPARATOR)

    mdfile.write(UNSUPERVISED)
    write_templates(mdfile, unsupervised)

    mdfile.write(SECTION_SEPARATOR)

    mdfile.write(RECOMMENDER_SYSTEMS)
    write_templates(mdfile, recommenders)

    mdfile.write(SECTION_SEPARATOR)

    mdfile.write(NLP)
    write_templates(mdfile, nlps)


if __name__ == "__main__":

    in_file = sys.argv[1]
    out_file = sys.argv[2]

    with open(in_file, 'r') as stream:
        templates = yaml.load(stream)
        parsed = [Template(position["template"]) for position in templates]

        with open(out_file, 'w') as mdfile:
            write_markdown(mdfile, parsed)
